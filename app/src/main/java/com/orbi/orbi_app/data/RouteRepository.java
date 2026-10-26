package com.orbi.orbi_app.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.orbi.orbi_app.data.remote.ApiClient;
import com.orbi.orbi_app.data.remote.OrbiApiService;
import com.orbi.orbi_app.data.remote.dto.DeliveryDto;
import com.orbi.orbi_app.data.remote.dto.DirectionsResponseDto;
import com.orbi.orbi_app.data.remote.dto.RouteResponseDto;
import com.orbi.orbi_app.model.Route;
import com.orbi.orbi_app.model.RouteStatus;
import com.orbi.orbi_app.model.Waypoint;
import com.orbi.orbi_app.model.WaypointStatus;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RouteRepository {
    private static RouteRepository instance;
    private final List<Route> cachedRoutes = new ArrayList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private RouteRepository() {
        initSampleData();
    }

    public static synchronized RouteRepository getInstance() {
        if (instance == null) {
            instance = new RouteRepository();
        }
        return instance;
    }

    private void initSampleData() {
        // Dados de resiliência e fallback alinhados com a Orbi API
        List<Waypoint> waypoints1 = new ArrayList<>();
        waypoints1.add(new Waypoint(1L, "1", 1, "Av. Paulista, 1578 - Bela Vista, São Paulo", -23.5614, -46.6559, WaypointStatus.IN_TRANSIT));
        waypoints1.add(new Waypoint(2L, "1", 2, "Rua Augusta, 1200 - Consolação, São Paulo", -23.5532, -46.6575, WaypointStatus.AT_PICKUP));
        waypoints1.add(new Waypoint(3L, "1", 3, "Rua da Consolação, 2400 - Cerqueira César, São Paulo", -23.5578, -46.6660, WaypointStatus.AT_PICKUP));
        waypoints1.add(new Waypoint(4L, "1", 4, "Al. Santos, 980 - Jardim Paulista, São Paulo", -23.5670, -46.6530, WaypointStatus.AT_PICKUP));

        cachedRoutes.add(new Route("1", "ROTA-ORBI-01", "driver_1", RouteStatus.IN_PROGRESS, 45, waypoints1));

        List<Waypoint> waypoints2 = new ArrayList<>();
        waypoints2.add(new Waypoint(5L, "2", 1, "Av. Brigadeiro Faria Lima, 3477 - Itaim Bibi, São Paulo", -23.5855, -46.6811, WaypointStatus.AT_PICKUP));
        waypoints2.add(new Waypoint(6L, "2", 2, "Rua Funchal, 418 - Vila Olímpia, São Paulo", -23.5930, -46.6890, WaypointStatus.AT_PICKUP));
        waypoints2.add(new Waypoint(7L, "2", 3, "Av. das Nações Unidas, 12555 - Brooklin, São Paulo", -23.6080, -46.6970, WaypointStatus.AT_PICKUP));

        cachedRoutes.add(new Route("2", "ROTA-ORBI-02", "driver_1", RouteStatus.ASSIGNED, 60, waypoints2));

        List<Waypoint> waypoints3 = new ArrayList<>();
        waypoints3.add(new Waypoint(8L, "3", 1, "Rua Oscar Freire, 850 - Pinheiros, São Paulo", -23.5645, -46.6710, WaypointStatus.CANCELLED));
        waypoints3.add(new Waypoint(9L, "3", 2, "Rua Teodoro Sampaio, 1400 - Pinheiros, São Paulo", -23.5590, -46.6800, WaypointStatus.CANCELLED));

        cachedRoutes.add(new Route("3", "ROTA-ORBI-03", "driver_1", RouteStatus.CANCELLED, 35, waypoints3));

        List<Waypoint> waypoints4 = new ArrayList<>();
        Waypoint c1 = new Waypoint(10L, "4", 1, "Rua Haddock Lobo, 595 - Cerqueira César, São Paulo", -23.5580, -46.6620, WaypointStatus.DELIVERED);
        c1.setCompletedAt(System.currentTimeMillis() - 7200000);
        Waypoint c2 = new Waypoint(11L, "4", 2, "Rua Bela Cintra, 1100 - Consolação, São Paulo", -23.5550, -46.6600, WaypointStatus.DELIVERED);
        c2.setCompletedAt(System.currentTimeMillis() - 3600000);
        waypoints4.add(c1);
        waypoints4.add(c2);

        cachedRoutes.add(new Route("4", "ROTA-ORBI-04", "driver_1", RouteStatus.COMPLETED, 30, waypoints4));
    }

    /**
     * Sincroniza rotas reais via GET /api/route da Orbi API, filtrando pelo motorista autenticado.
     */
    public void fetchRoutesFromApi(Context context, RepositoryCallback<List<Route>> callback) {
        SessionManager sessionManager = new SessionManager(context);
        String currentDriverUserId = sessionManager.getDriverUserId();

        OrbiApiService service = ApiClient.getApiService(context);
        service.getRoutes().enqueue(new Callback<List<RouteResponseDto>>() {
            @Override
            public void onResponse(Call<List<RouteResponseDto>> call, Response<List<RouteResponseDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<RouteResponseDto> dtos = response.body();
                    List<Route> convertedRoutes = new ArrayList<>();

                    cachedRoutes.clear();
                    for (RouteResponseDto dto : dtos) {
                        // Filtragem por motorista: exibe rotas atribuídas ao motorista ou não atribuídas se logado
                        if (!TextUtils.isEmpty(currentDriverUserId) && dto.getDriverInfo() != null) {
                            String assignedUserId = dto.getDriverInfo().getUserId();
                            if (!TextUtils.isEmpty(assignedUserId) && !assignedUserId.equalsIgnoreCase(currentDriverUserId)) {
                                continue;
                            }
                        }
                        Route r = convertDtoToRoute(dto, context);
                        convertedRoutes.add(r);
                        // Dispara busca das paradas reais em /api/deliveries/route/{routeId}
                        fetchDeliveriesByRouteIdFromApi(context, r.getId(), null);
                    }

                    if (!convertedRoutes.isEmpty()) {
                        cachedRoutes.addAll(convertedRoutes);
                    }

                    if (callback != null) {
                        mainHandler.post(() -> callback.onSuccess(getRoutes(RouteStatus.ALL)));
                    }
                } else {
                    String errorMsg = "Orbi API retornou erro HTTP " + response.code();
                    if (callback != null) mainHandler.post(() -> callback.onError(errorMsg));
                }
            }

            @Override
            public void onFailure(Call<List<RouteResponseDto>> call, Throwable t) {
                String errorMsg = "Falha de conexão com a API: " + t.getMessage();
                if (callback != null) mainHandler.post(() -> callback.onError(errorMsg));
            }
        });
    }

    /**
     * Consulta detalhes da rota específica via GET /api/route/{id} da Orbi API.
     */
    public void fetchRouteByIdFromApi(Context context, String routeId, RepositoryCallback<Route> callback) {
        long numericId = parseNumericId(routeId);
        OrbiApiService service = ApiClient.getApiService(context);

        service.getRouteById(numericId).enqueue(new Callback<RouteResponseDto>() {
            @Override
            public void onResponse(Call<RouteResponseDto> call, Response<RouteResponseDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Route updatedRoute = convertDtoToRoute(response.body(), context);

                    // Busca as entregas reais no endpoint GET /api/deliveries/route/{routeId}
                    service.getDeliveriesByRouteId(numericId).enqueue(new Callback<List<DeliveryDto>>() {
                        @Override
                        public void onResponse(Call<List<DeliveryDto>> callDel, Response<List<DeliveryDto>> respDel) {
                            if (respDel.isSuccessful() && respDel.body() != null) {
                                List<Waypoint> realWaypoints = convertDeliveryDtosToWaypoints(respDel.body(), routeId, context);
                                updatedRoute.setWaypoints(realWaypoints);
                            }
                            updateCacheRoute(routeId, updatedRoute);
                            if (callback != null) {
                                mainHandler.post(() -> callback.onSuccess(updatedRoute));
                            }
                        }

                        @Override
                        public void onFailure(Call<List<DeliveryDto>> callDel, Throwable t) {
                            updateCacheRoute(routeId, updatedRoute);
                            if (callback != null) {
                                mainHandler.post(() -> callback.onSuccess(updatedRoute));
                            }
                        }
                    });
                } else {
                    Route local = getRouteById(routeId);
                    if (local != null) {
                        if (callback != null) mainHandler.post(() -> callback.onSuccess(local));
                    } else {
                        if (callback != null) mainHandler.post(() -> callback.onError("Rota não encontrada na API (HTTP " + response.code() + ")"));
                    }
                }
            }

            @Override
            public void onFailure(Call<RouteResponseDto> call, Throwable t) {
                Route local = getRouteById(routeId);
                if (local != null) {
                    if (callback != null) mainHandler.post(() -> callback.onSuccess(local));
                } else {
                    if (callback != null) mainHandler.post(() -> callback.onError("Erro de conexão: " + t.getMessage()));
                }
            }
        });
    }

    private void updateCacheRoute(String routeId, Route updatedRoute) {
        boolean replaced = false;
        for (int i = 0; i < cachedRoutes.size(); i++) {
            if (cachedRoutes.get(i).getId().equals(routeId)) {
                cachedRoutes.set(i, updatedRoute);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            cachedRoutes.add(updatedRoute);
        }
    }

    /**
     * Consulta as paradas reais da rota diretamente via GET /api/deliveries/route/{routeId} da Orbi API.
     */
    public void fetchDeliveriesByRouteIdFromApi(Context context, String routeId, RepositoryCallback<List<Waypoint>> callback) {
        long numericId = parseNumericId(routeId);
        OrbiApiService service = ApiClient.getApiService(context);

        service.getDeliveriesByRouteId(numericId).enqueue(new Callback<List<DeliveryDto>>() {
            @Override
            public void onResponse(Call<List<DeliveryDto>> call, Response<List<DeliveryDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Waypoint> realWaypoints = convertDeliveryDtosToWaypoints(response.body(), routeId, context);

                    Route r = getRouteById(routeId);
                    if (r != null) {
                        r.setWaypoints(realWaypoints);
                    }

                    if (callback != null) {
                        mainHandler.post(() -> callback.onSuccess(realWaypoints));
                    }
                } else {
                    Route local = getRouteById(routeId);
                    if (local != null && local.getWaypoints() != null && !local.getWaypoints().isEmpty()) {
                        if (callback != null) mainHandler.post(() -> callback.onSuccess(local.getWaypoints()));
                    } else {
                        if (callback != null) mainHandler.post(() -> callback.onError("Nenhuma entrega encontrada para a rota (HTTP " + response.code() + ")"));
                    }
                }
            }

            @Override
            public void onFailure(Call<List<DeliveryDto>> call, Throwable t) {
                Route local = getRouteById(routeId);
                if (local != null && local.getWaypoints() != null && !local.getWaypoints().isEmpty()) {
                    if (callback != null) mainHandler.post(() -> callback.onSuccess(local.getWaypoints()));
                } else {
                    if (callback != null) mainHandler.post(() -> callback.onError("Erro de conexão ao buscar entregas: " + t.getMessage()));
                }
            }
        });
    }

    public List<Waypoint> convertDeliveryDtosToWaypoints(List<DeliveryDto> dtos, String routeId, Context context) {
        List<Waypoint> waypoints = new ArrayList<>();
        if (dtos == null) return waypoints;

        for (DeliveryDto d : dtos) {
            WaypointStatus ws = mapOrbiDeliveryStatus(d.getStatus());
            long deliveryId = d.getId() != null ? d.getId() : (long) d.getOrder();
            int order = d.getOrder() != null ? d.getOrder() : 1;
            String address = !TextUtils.isEmpty(d.getAddress()) ? d.getAddress() : "Endereço não informado";

            double lat = (d.getLatitude() != null && Math.abs(d.getLatitude()) > 0.0001) ? d.getLatitude() : -23.550520;
            double lon = (d.getLongitude() != null && Math.abs(d.getLongitude()) > 0.0001) ? d.getLongitude() : -46.633308;

            Waypoint wp = new Waypoint(
                    deliveryId,
                    routeId,
                    order,
                    address,
                    lat,
                    lon,
                    ws
            );

            if (!TextUtils.isEmpty(d.getRecipientEmail())) {
                wp.setRecipientName(d.getRecipientEmail());
            } else {
                wp.setRecipientName("Parada " + order);
            }

            // Se veio sem coordenadas reais e possui endereço textual, refina com geocoder em background
            boolean hasCoordinates = d.getLatitude() != null && d.getLongitude() != null 
                    && (Math.abs(d.getLatitude()) > 0.0001 || Math.abs(d.getLongitude()) > 0.0001);

            if (!hasCoordinates && context != null && !TextUtils.isEmpty(d.getAddress())) {
                AddressGeocoder.geocodeAddress(context, d.getAddress(), new AddressGeocoder.GeocodeCallback() {
                    @Override
                    public void onGeocoded(double latitude, double longitude) {
                        wp.setLatitude(latitude);
                        wp.setLongitude(longitude);
                    }

                    @Override
                    public void onError(String error) {
                    }
                });
            }

            waypoints.add(wp);
        }
        return waypoints;
    }

    /**
     * Avança o status da entrega via PATCH /api/deliveries/{id}/next-status da Orbi API.
     * Segue o ciclo da Orbi API: AT_PICKUP -> IN_TRANSIT -> DELIVERED.
     */
    public void advanceDelivery(Context context, String routeId, long deliveryId, RepositoryCallback<Boolean> callback) {
        Route route = getRouteById(routeId);
        Waypoint target = null;
        if (route != null) {
            for (Waypoint w : route.getWaypoints()) {
                if (w.getDeliveryId() == deliveryId) {
                    target = w;
                    break;
                }
            }
        }

        final Waypoint finalTarget = target;
        if (finalTarget != null) {
            // Transição conforme a máquina de estados oficial da Orbi API
            if (finalTarget.isAtPickup()) {
                finalTarget.setStatus(WaypointStatus.IN_TRANSIT);
            } else if (finalTarget.isInTransit()) {
                finalTarget.setStatus(WaypointStatus.DELIVERED);
                finalTarget.setCompletedAt(System.currentTimeMillis());
            } else {
                finalTarget.setStatus(WaypointStatus.DELIVERED);
                finalTarget.setCompletedAt(System.currentTimeMillis());
            }

            // Se todas as paradas foram tratadas, marca a rota como concluída
            if (route != null && route.areAllWaypointsHandled()) {
                route.setStatus(RouteStatus.COMPLETED);
            }

            OrbiApiService service = ApiClient.getApiService(context);
            service.advanceDeliveryStatus(deliveryId).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    mainHandler.post(() -> callback.onSuccess(true));
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    mainHandler.post(() -> callback.onSuccess(true)); // mantém atualização local
                }
            });
        } else {
            callback.onError("Parada não encontrada");
        }
    }

    /**
     * Cancela a entrega via PATCH /api/deliveries/{id}/cancel da Orbi API com registro de justificativa.
     */
    public void cancelDelivery(Context context, String routeId, long deliveryId, String justification, RepositoryCallback<Boolean> callback) {
        Route route = getRouteById(routeId);
        Waypoint target = null;
        if (route != null) {
            for (Waypoint w : route.getWaypoints()) {
                if (w.getDeliveryId() == deliveryId) {
                    target = w;
                    break;
                }
            }
        }

        final Waypoint finalTarget = target;
        if (finalTarget != null) {
            finalTarget.setStatus(WaypointStatus.CANCELLED);
            finalTarget.setJustification(justification);

            if (route != null && route.areAllWaypointsHandled()) {
                route.setStatus(RouteStatus.COMPLETED);
            }

            OrbiApiService service = ApiClient.getApiService(context);
            service.cancelDelivery(deliveryId).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    mainHandler.post(() -> callback.onSuccess(true));
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    mainHandler.post(() -> callback.onSuccess(true));
                }
            });
        } else {
            callback.onError("Parada não encontrada");
        }
    }

    /**
     * Pula a parada enviando o endereço para o final da fila de pendentes da rota.
     */
    public void postponeDelivery(Context context, String routeId, long deliveryId, RepositoryCallback<Boolean> callback) {
        Route route = getRouteById(routeId);
        if (route != null) {
            boolean postponed = route.postponeWaypoint(deliveryId);
            if (postponed) {
                mainHandler.post(() -> callback.onSuccess(true));
                return;
            }
        }
        mainHandler.post(() -> callback.onError("Parada não encontrada"));
    }

    /**
     * Mantém compatibilidade com chamadas anteriores de Skip redirecionando para o adiamento da fila.
     */
    public void skipWaypoint(Context context, String routeId, long deliveryId, String justification, RepositoryCallback<Boolean> callback) {
        postponeDelivery(context, routeId, deliveryId, callback);
    }

    /**
     * Obtém direções e traçado GeoJSON via GET /api/route/{id}/directions da Orbi API.
     */
    public void fetchRouteDirections(Context context, String routeId, RepositoryCallback<DirectionsResponseDto> callback) {
        long numericId = parseNumericId(routeId);
        OrbiApiService service = ApiClient.getApiService(context);

        service.getRouteDirections(numericId).enqueue(new Callback<DirectionsResponseDto>() {
            @Override
            public void onResponse(Call<DirectionsResponseDto> call, Response<DirectionsResponseDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    mainHandler.post(() -> callback.onSuccess(response.body()));
                } else {
                    mainHandler.post(() -> callback.onError("Erro na consulta de direções: HTTP " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<DirectionsResponseDto> call, Throwable t) {
                mainHandler.post(() -> callback.onError("Falha ao buscar direções: " + t.getMessage()));
            }
        });
    }

    private Route convertDtoToRoute(RouteResponseDto dto, Context context) {
        String routeIdStr = String.valueOf(dto.getId());
        Route existingRoute = getRouteById(routeIdStr);

        List<Waypoint> waypoints = new ArrayList<>();
        if (dto.getDeliveries() != null && !dto.getDeliveries().isEmpty()) {
            waypoints.addAll(convertDeliveryDtosToWaypoints(dto.getDeliveries(), routeIdStr, context));
        } else if (existingRoute != null && existingRoute.getWaypoints() != null && !existingRoute.getWaypoints().isEmpty()) {
            // Preserva paradas já obtidas anteriormente da API
            waypoints.addAll(existingRoute.getWaypoints());
        }
        // Paradas da rota agora são sincronizadas estritamente através do endpoint /api/deliveries/route/{routeId}

        RouteStatus rs = mapOrbiRouteStatus(dto.getStatus());
        int estDuration = (int) Math.round(dto.getTotalDistance() * 2.5);
        if (estDuration <= 0) estDuration = 30;

        return new Route(
                routeIdStr,
                "ROTA-ORBI-" + dto.getId(),
                dto.getDriverInfo() != null ? dto.getDriverInfo().getUserId() : "",
                rs,
                estDuration,
                waypoints
        );
    }

    /**
     * Retorna a próxima rota ativa da lista (ignorando rotas com status COMPLETED ou CANCELLED).
     */
    public Route getNextActiveRoute(String currentRouteId) {
        int currentIndex = -1;
        for (int i = 0; i < cachedRoutes.size(); i++) {
            if (cachedRoutes.get(i).getId().equals(currentRouteId)) {
                currentIndex = i;
                break;
            }
        }

        // 1. Procura a partir da posição da rota atual para frente
        for (int i = currentIndex + 1; i < cachedRoutes.size(); i++) {
            Route r = cachedRoutes.get(i);
            if (!r.getStatus().isTerminal() && !r.areAllWaypointsHandled()) {
                return r;
            }
        }

        // 2. Se não encontrou depois, busca a partir do início
        for (int i = 0; i <= currentIndex && i < cachedRoutes.size(); i++) {
            Route r = cachedRoutes.get(i);
            if (!r.getStatus().isTerminal() && !r.areAllWaypointsHandled() && !r.getId().equals(currentRouteId)) {
                return r;
            }
        }

        return null;
    }

    public List<Route> getRoutes(RouteStatus filter) {
        List<Route> result = new ArrayList<>();
        for (Route r : cachedRoutes) {
            if (filter == RouteStatus.COMPLETED) {
                if (r.getStatus() == RouteStatus.COMPLETED) {
                    result.add(r);
                }
            } else if (filter == RouteStatus.CANCELLED) {
                if (r.getStatus() == RouteStatus.CANCELLED) {
                    result.add(r);
                }
            } else if (filter == RouteStatus.ASSIGNED) {
                if (r.getStatus() == RouteStatus.ASSIGNED || r.getStatus() == RouteStatus.PENDING) {
                    result.add(r);
                }
            } else if (filter == RouteStatus.ALL) {
                // Em "Todas", rotas finalizadas (COMPLETED ou CANCELLED) ficam ocultas por padrão
                if (!r.getStatus().isTerminal()) {
                    result.add(r);
                }
            } else {
                if (r.getStatus() == filter) {
                    result.add(r);
                }
            }
        }
        return result;
    }

    public Route getRouteById(String id) {
        for (Route r : cachedRoutes) {
            if (r.getId().equals(id)) {
                return r;
            }
        }
        return null;
    }

    public boolean startRoute(String routeId) {
        Route route = getRouteById(routeId);
        if (route != null) {
            route.setStatus(RouteStatus.IN_PROGRESS);
            return true;
        }
        return false;
    }

    public boolean completeRoute(String routeId) {
        Route route = getRouteById(routeId);
        if (route != null) {
            route.setStatus(RouteStatus.COMPLETED);
            return true;
        }
        return false;
    }

    private long parseNumericId(String id) {
        try {
            return Long.parseLong(id.replaceAll("\\D+", ""));
        } catch (Exception e) {
            return 1L;
        }
    }

    private WaypointStatus mapOrbiDeliveryStatus(String status) {
        if (status == null) return WaypointStatus.AT_PICKUP;
        switch (status.toUpperCase()) {
            case "DELIVERED":
            case "COMPLETED":
                return WaypointStatus.DELIVERED;
            case "CANCELLED":
                return WaypointStatus.CANCELLED;
            case "IN_TRANSIT":
            case "OUT_FOR_DELIVERY":
                return WaypointStatus.IN_TRANSIT;
            case "TO_BE_DELIVERED":
            case "AT_PICKUP":
            case "REQUESTED":
            case "ACCEPTED":
            default:
                return WaypointStatus.AT_PICKUP;
        }
    }

    private RouteStatus mapOrbiRouteStatus(String status) {
        if (status == null) return RouteStatus.ASSIGNED;
        switch (status.toUpperCase()) {
            case "IN_PROGRESS":
                return RouteStatus.IN_PROGRESS;
            case "COMPLETED":
                return RouteStatus.COMPLETED;
            case "CANCELLED":
                return RouteStatus.CANCELLED;
            case "PENDING":
                return RouteStatus.PENDING;
            case "CREATED":
            case "ASSIGNED":
            default:
                return RouteStatus.ASSIGNED;
        }
    }

    private List<Waypoint> generateFallbackWaypoints(String routeId, int stops) {
        List<Waypoint> list = new ArrayList<>();
        String[][] sampleAddresses = {
                {"Av. Paulista, 1578 - Bela Vista, São Paulo", "-23.5614", "-46.6559", "Carlos Silva", "(11) 98765-4321"},
                {"Rua Augusta, 1200 - Consolação, São Paulo", "-23.5532", "-46.6575", "Mariana Costa", "(11) 97654-3210"},
                {"Rua da Consolação, 2400 - Cerqueira César, São Paulo", "-23.5578", "-46.6660", "Roberto Souza", "(11) 96543-2109"},
                {"Al. Santos, 980 - Jardim Paulista, São Paulo", "-23.5670", "-46.6530", "Ana Paula Ramos", "(11) 95432-1098"},
                {"Av. Brigadeiro Faria Lima, 3477 - Itaim Bibi, São Paulo", "-23.5855", "-46.6811", "Lucas Fernandes", "(11) 94321-0987"},
                {"Rua Funchal, 418 - Vila Olímpia, São Paulo", "-23.5930", "-46.6890", "Beatriz Lima", "(11) 93210-9876"},
                {"Av. das Nações Unidas, 12555 - Brooklin, São Paulo", "-23.6080", "-46.6970", "Juliana Martins", "(11) 92109-8765"},
                {"Rua Oscar Freire, 850 - Pinheiros, São Paulo", "-23.5645", "-46.6710", "Gabriel Oliveira", "(11) 91098-7654"}
        };

        int count = Math.max(1, Math.min(stops, 10));
        long baseDeliveryId = parseNumericId(routeId) * 100L;

        for (int i = 0; i < count; i++) {
            String[] sample = sampleAddresses[i % sampleAddresses.length];
            long deliveryId = baseDeliveryId + (i + 1);
            double lat = Double.parseDouble(sample[1]);
            double lng = Double.parseDouble(sample[2]);
            Waypoint wp = new Waypoint(
                    String.valueOf(deliveryId),
                    routeId,
                    i + 1,
                    sample[3],
                    sample[4],
                    sample[0],
                    "",
                    "",
                    "São Paulo",
                    lat,
                    lng,
                    WaypointStatus.AT_PICKUP
            );
            list.add(wp);
        }
        return list;
    }
}
