package com.orbi.orbi_app.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

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
        // Dados de resiliência e fallback
        List<Waypoint> waypoints1 = new ArrayList<>();
        waypoints1.add(new Waypoint(1L, "1", 1, "Av. Paulista, 1578 - Bela Vista, São Paulo", -23.5614, -46.6559, WaypointStatus.IN_TRANSIT));
        waypoints1.add(new Waypoint(2L, "1", 2, "Rua Augusta, 1200 - Consolação, São Paulo", -23.5532, -46.6575, WaypointStatus.PENDING));
        waypoints1.add(new Waypoint(3L, "1", 3, "Rua da Consolação, 2400 - Cerqueira César, São Paulo", -23.5578, -46.6660, WaypointStatus.PENDING));
        waypoints1.add(new Waypoint(4L, "1", 4, "Al. Santos, 980 - Jardim Paulista, São Paulo", -23.5670, -46.6530, WaypointStatus.PENDING));

        cachedRoutes.add(new Route("1", "ROTA-ORBI-01", "driver_1", RouteStatus.IN_PROGRESS, 45, waypoints1));

        List<Waypoint> waypoints2 = new ArrayList<>();
        waypoints2.add(new Waypoint(5L, "2", 1, "Av. Brigadeiro Faria Lima, 3477 - Itaim Bibi, São Paulo", -23.5855, -46.6811, WaypointStatus.PENDING));
        waypoints2.add(new Waypoint(6L, "2", 2, "Rua Funchal, 418 - Vila Olímpia, São Paulo", -23.5930, -46.6890, WaypointStatus.PENDING));
        waypoints2.add(new Waypoint(7L, "2", 3, "Av. das Nações Unidas, 12555 - Brooklin, São Paulo", -23.6080, -46.6970, WaypointStatus.PENDING));

        cachedRoutes.add(new Route("2", "ROTA-ORBI-02", "driver_1", RouteStatus.PENDING, 60, waypoints2));

        List<Waypoint> waypoints3 = new ArrayList<>();
        waypoints3.add(new Waypoint(8L, "3", 1, "Rua Oscar Freire, 850 - Pinheiros, São Paulo", -23.5645, -46.6710, WaypointStatus.PENDING));
        waypoints3.add(new Waypoint(9L, "3", 2, "Rua Teodoro Sampaio, 1400 - Pinheiros, São Paulo", -23.5590, -46.6800, WaypointStatus.PENDING));

        cachedRoutes.add(new Route("3", "ROTA-ORBI-03", "driver_1", RouteStatus.DELAYED, 35, waypoints3));

        List<Waypoint> waypoints4 = new ArrayList<>();
        Waypoint c1 = new Waypoint(10L, "4", 1, "Rua Haddock Lobo, 595 - Cerqueira César, São Paulo", -23.5580, -46.6620, WaypointStatus.COMPLETED);
        c1.setCompletedAt(System.currentTimeMillis() - 7200000);
        Waypoint c2 = new Waypoint(11L, "4", 2, "Rua Bela Cintra, 1100 - Consolação, São Paulo", -23.5550, -46.6600, WaypointStatus.COMPLETED);
        c2.setCompletedAt(System.currentTimeMillis() - 3600000);
        waypoints4.add(c1);
        waypoints4.add(c2);

        cachedRoutes.add(new Route("4", "ROTA-ORBI-04", "driver_1", RouteStatus.COMPLETED, 30, waypoints4));
    }

    /**
     * Sincroniza rotas reais via GET /api/route da Orbi API.
     */
    public void fetchRoutesFromApi(Context context, RepositoryCallback<List<Route>> callback) {
        OrbiApiService service = ApiClient.getApiService(context);
        service.getRoutes().enqueue(new Callback<List<RouteResponseDto>>() {
            @Override
            public void onResponse(Call<List<RouteResponseDto>> call, Response<List<RouteResponseDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<RouteResponseDto> dtos = response.body();
                    List<Route> convertedRoutes = new ArrayList<>();

                    for (RouteResponseDto dto : dtos) {
                        convertedRoutes.add(convertDtoToRoute(dto));
                    }

                    if (!convertedRoutes.isEmpty()) {
                        cachedRoutes.clear();
                        cachedRoutes.addAll(convertedRoutes);
                    }

                    mainHandler.post(() -> callback.onSuccess(getRoutes(RouteStatus.ALL)));
                } else {
                    String errorMsg = "Orbi API retornou erro HTTP " + response.code();
                    mainHandler.post(() -> callback.onError(errorMsg));
                }
            }

            @Override
            public void onFailure(Call<List<RouteResponseDto>> call, Throwable t) {
                String errorMsg = "Falha de conexão com a API: " + t.getMessage();
                mainHandler.post(() -> callback.onError(errorMsg));
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
                    Route updatedRoute = convertDtoToRoute(response.body());

                    // Atualiza no cache
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

                    mainHandler.post(() -> callback.onSuccess(updatedRoute));
                } else {
                    Route local = getRouteById(routeId);
                    if (local != null) {
                        mainHandler.post(() -> callback.onSuccess(local));
                    } else {
                        mainHandler.post(() -> callback.onError("Rota não encontrada na API (HTTP " + response.code() + ")"));
                    }
                }
            }

            @Override
            public void onFailure(Call<RouteResponseDto> call, Throwable t) {
                Route local = getRouteById(routeId);
                if (local != null) {
                    mainHandler.post(() -> callback.onSuccess(local));
                } else {
                    mainHandler.post(() -> callback.onError("Erro de conexão: " + t.getMessage()));
                }
            }
        });
    }

    /**
     * Avança o status da entrega via PATCH /api/deliveries/{id}/next-status da Orbi API.
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
            finalTarget.setStatus(WaypointStatus.COMPLETED);
            finalTarget.setCompletedAt(System.currentTimeMillis());

            Waypoint next = route.getNextPendingWaypoint();
            if (next != null) {
                next.setStatus(WaypointStatus.IN_TRANSIT);
            }

            OrbiApiService service = ApiClient.getApiService(context);
            service.advanceDeliveryStatus(deliveryId).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        mainHandler.post(() -> callback.onSuccess(true));
                    } else {
                        mainHandler.post(() -> callback.onSuccess(true)); // mantém local
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    mainHandler.post(() -> callback.onSuccess(true)); // mantém local
                }
            });
        } else {
            callback.onError("Parada não encontrada");
        }
    }

    /**
     * Cancela a entrega via PATCH /api/deliveries/{id}/cancel da Orbi API.
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

            Waypoint next = route.getNextPendingWaypoint();
            if (next != null) {
                next.setStatus(WaypointStatus.IN_TRANSIT);
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
     * Pula a parada da rota localmente registrando justificativa.
     */
    public void skipWaypoint(Context context, String routeId, long deliveryId, String justification, RepositoryCallback<Boolean> callback) {
        Route route = getRouteById(routeId);
        if (route != null) {
            for (Waypoint w : route.getWaypoints()) {
                if (w.getDeliveryId() == deliveryId) {
                    w.setStatus(WaypointStatus.SKIPPED);
                    w.setJustification(justification);

                    Waypoint next = route.getNextPendingWaypoint();
                    if (next != null) {
                        next.setStatus(WaypointStatus.IN_TRANSIT);
                    }
                    callback.onSuccess(true);
                    return;
                }
            }
        }
        callback.onError("Parada não encontrada");
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

    private Route convertDtoToRoute(RouteResponseDto dto) {
        List<Waypoint> waypoints = new ArrayList<>();
        if (dto.getDeliveries() != null) {
            for (DeliveryDto d : dto.getDeliveries()) {
                WaypointStatus ws = mapOrbiDeliveryStatus(d.getStatus());
                Waypoint wp = new Waypoint(
                        d.getId(),
                        String.valueOf(dto.getId()),
                        d.getOrder(),
                        d.getAddress(),
                        -23.5505 + (d.getOrder() * 0.005),
                        -46.6333 - (d.getOrder() * 0.004),
                        ws
                );
                waypoints.add(wp);
            }
        }

        RouteStatus rs = mapOrbiRouteStatus(dto.getStatus());
        int estDuration = (int) Math.round(dto.getTotalDistance() * 2.5);
        if (estDuration <= 0) estDuration = 30;

        return new Route(
                String.valueOf(dto.getId()),
                "ROTA-ORBI-" + dto.getId(),
                dto.getDriverInfo() != null ? dto.getDriverInfo().getUserId() : "",
                rs,
                estDuration,
                waypoints
        );
    }

    public List<Route> getRoutes(RouteStatus filter) {
        List<Route> result = new ArrayList<>();
        for (Route r : cachedRoutes) {
            if (filter == RouteStatus.COMPLETED) {
                if (r.getStatus() == RouteStatus.COMPLETED) {
                    result.add(r);
                }
            } else if (filter == RouteStatus.ALL) {
                if (r.getStatus() != RouteStatus.COMPLETED) {
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
            Waypoint next = route.getNextPendingWaypoint();
            if (next != null) {
                next.setStatus(WaypointStatus.IN_TRANSIT);
            }
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
        if (status == null) return WaypointStatus.PENDING;
        switch (status.toUpperCase()) {
            case "DELIVERED":
                return WaypointStatus.COMPLETED;
            case "CANCELLED":
                return WaypointStatus.CANCELLED;
            case "IN_TRANSIT":
                return WaypointStatus.IN_TRANSIT;
            default:
                return WaypointStatus.PENDING;
        }
    }

    private RouteStatus mapOrbiRouteStatus(String status) {
        if (status == null) return RouteStatus.PENDING;
        switch (status.toUpperCase()) {
            case "IN_PROGRESS":
                return RouteStatus.IN_PROGRESS;
            case "COMPLETED":
                return RouteStatus.COMPLETED;
            case "CANCELLED":
            case "DELAYED":
                return RouteStatus.DELAYED;
            case "ASSIGNED":
            case "PENDING":
            default:
                return RouteStatus.PENDING;
        }
    }
}
