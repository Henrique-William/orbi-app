package com.orbi.orbi_app.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.orbi.orbi_app.data.remote.ApiClient;
import com.orbi.orbi_app.data.remote.OrbiApiService;
import com.orbi.orbi_app.data.remote.dto.DeliveryDetailsDto;
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
        cachedRoutes.clear();
    }

    public static synchronized RouteRepository getInstance() {
        if (instance == null) {
            instance = new RouteRepository();
        }
        return instance;
    }

    public synchronized void resetForNewSession() {
        cachedRoutes.clear();
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
                    }

                    cachedRoutes.clear();
                    cachedRoutes.addAll(convertedRoutes);

                    if (callback != null) {
                        mainHandler.post(() -> callback.onSuccess(getRoutes(RouteStatus.ALL)));
                    }

                    // Busca as paradas reais de cada rota em /api/deliveries/route/{routeId}
                    for (Route r : convertedRoutes) {
                        fetchDeliveriesByRouteIdFromApi(context, r.getId(), new RepositoryCallback<List<Waypoint>>() {
                            @Override
                            public void onSuccess(List<Waypoint> waypoints) {
                                if (callback != null) {
                                    mainHandler.post(() -> callback.onSuccess(getRoutes(RouteStatus.ALL)));
                                }
                            }

                            @Override
                            public void onError(String message) {
                            }
                        });
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
                        // Preserva estados terminais já atualizados localmente
                        if (r.getWaypoints() != null) {
                            for (Waypoint realWp : realWaypoints) {
                                for (Waypoint localWp : r.getWaypoints()) {
                                    if (localWp.getDeliveryId() == realWp.getDeliveryId() && localWp.isTerminal()) {
                                        realWp.setStatus(localWp.getStatus());
                                        realWp.setCompletedAt(localWp.getCompletedAt());
                                        realWp.setJustification(localWp.getJustification());
                                    }
                                }
                            }
                        }
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

            if (!TextUtils.isEmpty(d.getDetails())) {
                wp.setJustification(d.getDetails());
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
     * Conclui a entrega via PATCH /api/deliveries/{id}/delivered da Orbi API.
     * Caso o ID local esteja dessincronizado (404 "Entrega não encontrada"), resolve o ID real
     * via GET /api/deliveries/route/{routeId} e reexecuta a transição na API.
     */
    public void completeDelivery(Context context, String routeId, long deliveryId, RepositoryCallback<Boolean> callback) {
        Route route = getRouteById(routeId);
        Waypoint target = findTargetWaypoint(route, deliveryId);

        final Waypoint finalTarget = target;
        if (finalTarget != null) {
            OrbiApiService service = ApiClient.getApiService(context);
            executeCompleteDeliveryCall(context, service, routeId, route, finalTarget, finalTarget.getDeliveryId(), true, callback);
        } else {
            callback.onError("Parada não encontrada");
        }
    }

    private void executeCompleteDeliveryCall(
            Context context,
            OrbiApiService service,
            String routeId,
            Route route,
            Waypoint finalTarget,
            long targetDeliveryId,
            boolean allowResolveRetry,
            RepositoryCallback<Boolean> callback
    ) {
        service.completeDelivery(targetDeliveryId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    markWaypointDeliveredLocally(route, finalTarget);
                    mainHandler.post(() -> callback.onSuccess(true));
                } else if (allowResolveRetry && (response.code() == 404 || response.code() == 400)) {
                    resolveRealDeliveryIdFromRoute(context, service, routeId, route, finalTarget, resolvedId -> {
                        if (resolvedId > 0 && resolvedId != targetDeliveryId) {
                            executeCompleteDeliveryCall(context, service, routeId, route, finalTarget, resolvedId, false, callback);
                        } else {
                            long idToUse = resolvedId > 0 ? resolvedId : targetDeliveryId;
                            advanceToDeliveredViaNextStatus(service, route, finalTarget, idToUse, 3, callback);
                        }
                    });
                } else if (response.code() == 404 || response.code() == 405 || response.code() == 400) {
                    advanceToDeliveredViaNextStatus(service, route, finalTarget, targetDeliveryId, 3, callback);
                } else {
                    String errorMsg = "Erro na API: HTTP " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            String bodyStr = response.errorBody().string();
                            if (!bodyStr.isEmpty()) errorMsg += " (" + bodyStr + ")";
                        }
                    } catch (Exception ignored) {}
                    final String finalMsg = errorMsg;
                    mainHandler.post(() -> callback.onError(finalMsg));
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                markWaypointDeliveredLocally(route, finalTarget);
                mainHandler.post(() -> callback.onSuccess(true));
            }
        });
    }

    private void markWaypointDeliveredLocally(Route route, Waypoint waypoint) {
        waypoint.setStatus(WaypointStatus.DELIVERED);
        waypoint.setCompletedAt(System.currentTimeMillis());
        if (route != null && route.areAllWaypointsHandled()) {
            route.setStatus(RouteStatus.COMPLETED);
        }
    }

    private void advanceToDeliveredViaNextStatus(
            OrbiApiService service,
            Route route,
            Waypoint finalTarget,
            long deliveryId,
            int remainingSteps,
            RepositoryCallback<Boolean> callback
    ) {
        service.advanceDeliveryStatus(deliveryId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful() && remainingSteps > 1) {
                    advanceToDeliveredViaNextStatus(service, route, finalTarget, deliveryId, remainingSteps - 1, callback);
                } else {
                    markWaypointDeliveredLocally(route, finalTarget);
                    mainHandler.post(() -> callback.onSuccess(true));
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                markWaypointDeliveredLocally(route, finalTarget);
                mainHandler.post(() -> callback.onSuccess(true));
            }
        });
    }

    /**
     * Cancela a entrega via PATCH /api/deliveries/{id}/cancel da Orbi API com registro de justificativa.
     */
    public void cancelDelivery(Context context, String routeId, long deliveryId, String justification, RepositoryCallback<Boolean> callback) {
        Route route = getRouteById(routeId);
        Waypoint target = findTargetWaypoint(route, deliveryId);

        final Waypoint finalTarget = target;
        if (finalTarget != null) {
            finalTarget.setStatus(WaypointStatus.CANCELLED);
            finalTarget.setJustification(justification);

            if (route != null && route.areAllWaypointsHandled()) {
                route.setStatus(RouteStatus.COMPLETED);
            }

            OrbiApiService service = ApiClient.getApiService(context);
            DeliveryDetailsDto body = new DeliveryDetailsDto(justification);
            executeCancelDeliveryCall(context, service, routeId, route, finalTarget, finalTarget.getDeliveryId(), body, true, callback);
        } else {
            callback.onError("Parada não encontrada");
        }
    }

    private void executeCancelDeliveryCall(
            Context context,
            OrbiApiService service,
            String routeId,
            Route route,
            Waypoint finalTarget,
            long targetDeliveryId,
            DeliveryDetailsDto body,
            boolean allowResolveRetry,
            RepositoryCallback<Boolean> callback
    ) {
        service.cancelDelivery(targetDeliveryId, body).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    mainHandler.post(() -> callback.onSuccess(true));
                } else if (allowResolveRetry && (response.code() == 404 || response.code() == 400)) {
                    resolveRealDeliveryIdFromRoute(context, service, routeId, route, finalTarget, resolvedId -> {
                        if (resolvedId > 0 && resolvedId != targetDeliveryId) {
                            executeCancelDeliveryCall(context, service, routeId, route, finalTarget, resolvedId, body, false, callback);
                        } else {
                            mainHandler.post(() -> callback.onSuccess(true));
                        }
                    });
                } else if (response.code() == 404 || response.code() == 400) {
                    mainHandler.post(() -> callback.onSuccess(true));
                } else {
                    String errorMsg = "Erro na API: HTTP " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            String bodyStr = response.errorBody().string();
                            if (!bodyStr.isEmpty()) errorMsg += " (" + bodyStr + ")";
                        }
                    } catch (Exception ignored) {}
                    final String finalMsg = errorMsg;
                    mainHandler.post(() -> callback.onError(finalMsg));
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                mainHandler.post(() -> callback.onSuccess(true));
            }
        });
    }

    /**
     * Pula a entrega via PATCH /api/deliveries/{id}/skip da Orbi API com registro de justificativa nos details.
     */
    public void skipDelivery(Context context, String routeId, long deliveryId, String justification, RepositoryCallback<Boolean> callback) {
        Route route = getRouteById(routeId);
        Waypoint target = findTargetWaypoint(route, deliveryId);

        final Waypoint finalTarget = target;
        if (finalTarget != null) {
            finalTarget.setStatus(WaypointStatus.SKIPPED);
            finalTarget.setJustification(justification);

            if (route != null && route.areAllWaypointsHandled()) {
                route.setStatus(RouteStatus.COMPLETED);
            }

            OrbiApiService service = ApiClient.getApiService(context);
            DeliveryDetailsDto body = new DeliveryDetailsDto(justification);
            executeSkipDeliveryCall(context, service, routeId, route, finalTarget, finalTarget.getDeliveryId(), body, true, callback);
        } else {
            callback.onError("Parada não encontrada");
        }
    }

    private void executeSkipDeliveryCall(
            Context context,
            OrbiApiService service,
            String routeId,
            Route route,
            Waypoint finalTarget,
            long targetDeliveryId,
            DeliveryDetailsDto body,
            boolean allowResolveRetry,
            RepositoryCallback<Boolean> callback
    ) {
        service.skipDelivery(targetDeliveryId, body).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    mainHandler.post(() -> callback.onSuccess(true));
                } else if (allowResolveRetry && (response.code() == 404 || response.code() == 400)) {
                    resolveRealDeliveryIdFromRoute(context, service, routeId, route, finalTarget, resolvedId -> {
                        if (resolvedId > 0 && resolvedId != targetDeliveryId) {
                            executeSkipDeliveryCall(context, service, routeId, route, finalTarget, resolvedId, body, false, callback);
                        } else {
                            mainHandler.post(() -> callback.onSuccess(true));
                        }
                    });
                } else if (response.code() == 404 || response.code() == 400) {
                    mainHandler.post(() -> callback.onSuccess(true));
                } else {
                    String errorMsg = "Erro na API: HTTP " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            String bodyStr = response.errorBody().string();
                            if (!bodyStr.isEmpty()) errorMsg += " (" + bodyStr + ")";
                        }
                    } catch (Exception ignored) {}
                    final String finalMsg = errorMsg;
                    mainHandler.post(() -> callback.onError(finalMsg));
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                mainHandler.post(() -> callback.onSuccess(true));
            }
        });
    }

    private interface DeliveryIdResolverCallback {
        void onResolved(long realDeliveryId);
    }

    private Waypoint findTargetWaypoint(Route route, long deliveryId) {
        if (route == null || route.getWaypoints() == null) return null;
        for (Waypoint w : route.getWaypoints()) {
            if (w.getDeliveryId() == deliveryId) {
                return w;
            }
        }
        return route.getNextPendingWaypoint();
    }

    /**
     * Consulta GET /api/deliveries/route/{routeId} para sincronizar os IDs reais do banco de dados
     * quando uma chamada PATCH retorna 404 por dessincronização de IDs locais.
     */
    private void resolveRealDeliveryIdFromRoute(
            Context context,
            OrbiApiService service,
            String routeId,
            Route route,
            Waypoint targetWaypoint,
            DeliveryIdResolverCallback resolverCallback
    ) {
        long numericRouteId = parseNumericId(routeId);
        service.getDeliveriesByRouteId(numericRouteId).enqueue(new Callback<List<DeliveryDto>>() {
            @Override
            public void onResponse(Call<List<DeliveryDto>> call, Response<List<DeliveryDto>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    long resolved = matchAndSyncDeliveryIds(response.body(), route, targetWaypoint);
                    resolverCallback.onResolved(resolved);
                } else {
                    service.getRouteDeliveries(numericRouteId).enqueue(new Callback<List<DeliveryDto>>() {
                        @Override
                        public void onResponse(Call<List<DeliveryDto>> call2, Response<List<DeliveryDto>> resp2) {
                            if (resp2.isSuccessful() && resp2.body() != null && !resp2.body().isEmpty()) {
                                long resolved = matchAndSyncDeliveryIds(resp2.body(), route, targetWaypoint);
                                resolverCallback.onResolved(resolved);
                            } else {
                                resolverCallback.onResolved(-1L);
                            }
                        }

                        @Override
                        public void onFailure(Call<List<DeliveryDto>> call2, Throwable t) {
                            resolverCallback.onResolved(-1L);
                        }
                    });
                }
            }

            @Override
            public void onFailure(Call<List<DeliveryDto>> call, Throwable t) {
                resolverCallback.onResolved(-1L);
            }
        });
    }

    private long matchAndSyncDeliveryIds(List<DeliveryDto> dtos, Route route, Waypoint targetWaypoint) {
        if (dtos == null || dtos.isEmpty()) return -1L;

        // Sincroniza todos os deliveryIds das paradas da rota pela ordem (order)
        if (route != null && route.getWaypoints() != null) {
            for (Waypoint wp : route.getWaypoints()) {
                for (DeliveryDto d : dtos) {
                    if (d.getId() != null && d.getOrder() != null && d.getOrder() == wp.getSequenceOrder()) {
                        wp.setDeliveryId(d.getId());
                        break;
                    }
                }
            }
        }

        if (targetWaypoint != null) {
            // 1. Busca pela ordem exata da parada na rota
            for (DeliveryDto d : dtos) {
                if (d.getId() != null && d.getOrder() != null && d.getOrder() == targetWaypoint.getSequenceOrder()) {
                    targetWaypoint.setDeliveryId(d.getId());
                    return d.getId();
                }
            }
            // 2. Busca pelo endereço correspondente
            String targetAddr = targetWaypoint.getFullAddress() != null ? targetWaypoint.getFullAddress().trim() : "";
            for (DeliveryDto d : dtos) {
                if (d.getId() != null && d.getAddress() != null && d.getAddress().trim().equalsIgnoreCase(targetAddr)) {
                    targetWaypoint.setDeliveryId(d.getId());
                    return d.getId();
                }
            }
            // 3. Fallback: primeira entrega pendente da rota no servidor
            for (DeliveryDto d : dtos) {
                WaypointStatus ws = mapOrbiDeliveryStatus(d.getStatus());
                if (d.getId() != null && !ws.isTerminal()) {
                    targetWaypoint.setDeliveryId(d.getId());
                    return d.getId();
                }
            }
        }
        return -1L;
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

        String routeCode = (dto.getName() != null && !dto.getName().trim().isEmpty())
                ? dto.getName().trim()
                : "ROTA-ORBI-" + dto.getId();

        return new Route(
                routeIdStr,
                routeCode,
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
        return startRoute(null, routeId);
    }

    public boolean startRoute(Context context, String routeId) {
        Route route = getRouteById(routeId);
        if (route != null) {
            boolean wasAssigned = route.getStatus() == RouteStatus.ASSIGNED || route.getStatus() == RouteStatus.PENDING;
            route.setStatus(RouteStatus.IN_PROGRESS);
            if (context != null && wasAssigned) {
                long numericId = parseNumericId(routeId);
                ApiClient.getApiService(context).advanceRouteStatus(numericId).enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                    }
                });
            }
            return true;
        }
        return false;
    }

    public boolean completeRoute(String routeId) {
        return completeRoute(null, routeId);
    }

    public boolean completeRoute(Context context, String routeId) {
        Route route = getRouteById(routeId);
        if (route != null) {
            boolean wasInProgress = route.getStatus() == RouteStatus.IN_PROGRESS;
            route.setStatus(RouteStatus.COMPLETED);
            if (context != null && wasInProgress) {
                long numericId = parseNumericId(routeId);
                ApiClient.getApiService(context).advanceRouteStatus(numericId).enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                    }
                });
            }
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
            case "SKIPPED":
                return WaypointStatus.SKIPPED;
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
}
