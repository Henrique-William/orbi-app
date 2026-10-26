package com.orbi.orbi_app;

import static org.junit.Assert.*;

import com.orbi.orbi_app.model.Route;
import com.orbi.orbi_app.model.RouteStatus;
import com.orbi.orbi_app.model.Waypoint;
import com.orbi.orbi_app.model.WaypointStatus;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class RouteWorkflowTest {

    @Test
    public void testEmptyWaypointsIsNotHandled() {
        Route route = new Route("1", "ROTA-01", "driver_1", RouteStatus.ASSIGNED, 30, new ArrayList<>());
        assertFalse("Rota sem paradas não deve ser considerada concluída/tratada", route.areAllWaypointsHandled());
        assertNull("Não deve retornar próxima parada em rota vazia", route.getNextPendingWaypoint());
    }

    @Test
    public void testWaypointsStatusDetection() {
        List<Waypoint> waypoints = new ArrayList<>();
        waypoints.add(new Waypoint(101L, "1", 1, "Av. Paulista, 1000", -23.56, -46.65, WaypointStatus.AT_PICKUP));
        waypoints.add(new Waypoint(102L, "1", 2, "Rua Augusta, 500", -23.55, -46.65, WaypointStatus.AT_PICKUP));

        Route route = new Route("1", "ROTA-01", "driver_1", RouteStatus.ASSIGNED, 30, waypoints);
        assertFalse("Com paradas AT_PICKUP, areAllWaypointsHandled deve ser false", route.areAllWaypointsHandled());
        assertEquals(101L, route.getNextPendingWaypoint().getDeliveryId());

        // Avança parada 1 para DELIVERED
        waypoints.get(0).setStatus(WaypointStatus.DELIVERED);
        assertFalse(route.areAllWaypointsHandled());
        assertEquals("Deve avançar para a parada 2", 102L, route.getNextPendingWaypoint().getDeliveryId());

        // Avança parada 2 para CANCELLED
        waypoints.get(1).setStatus(WaypointStatus.CANCELLED);
        assertTrue("Todas tratadas (DELIVERED e CANCELLED)", route.areAllWaypointsHandled());
        assertNull("Nenhuma parada pendente restante", route.getNextPendingWaypoint());
    }

    @Test
    public void testPostponeWaypointMovesToEndOfQueue() {
        List<Waypoint> waypoints = new ArrayList<>();
        waypoints.add(new Waypoint(101L, "1", 1, "Ponto 1", -23.56, -46.65, WaypointStatus.AT_PICKUP));
        waypoints.add(new Waypoint(102L, "1", 2, "Ponto 2", -23.55, -46.65, WaypointStatus.AT_PICKUP));
        waypoints.add(new Waypoint(103L, "1", 3, "Ponto 3", -23.54, -46.64, WaypointStatus.AT_PICKUP));

        Route route = new Route("1", "ROTA-01", "driver_1", RouteStatus.IN_PROGRESS, 30, waypoints);

        // Pula o ponto 1 para o final da fila
        boolean postponed = route.postponeWaypoint(101L);
        assertTrue(postponed);

        assertEquals("Primeiro da fila agora deve ser o ponto 2", 102L, route.getWaypoints().get(0).getDeliveryId());
        assertEquals(1, route.getWaypoints().get(0).getSequenceOrder());

        assertEquals("Segundo da fila agora deve ser o ponto 3", 103L, route.getWaypoints().get(1).getDeliveryId());
        assertEquals(2, route.getWaypoints().get(1).getSequenceOrder());

        assertEquals("Último da fila agora deve ser o ponto 1 adiado", 101L, route.getWaypoints().get(2).getDeliveryId());
        assertEquals(3, route.getWaypoints().get(2).getSequenceOrder());

        // Próxima parada pendente agora é a 102
        assertEquals(102L, route.getNextPendingWaypoint().getDeliveryId());
    }

    @Test
    public void testButtonWorkflowResolution() {
        // Cenário 1: Rota ainda não iniciada (ASSIGNED)
        List<Waypoint> waypoints1 = new ArrayList<>();
        waypoints1.add(new Waypoint(101L, "1", 1, "Ponto 1", -23.56, -46.65, WaypointStatus.AT_PICKUP));
        Route routeNotStarted = new Route("1", "ROTA-01", "driver_1", RouteStatus.ASSIGNED, 30, waypoints1);

        assertFalse(routeNotStarted.areAllWaypointsHandled());
        assertEquals(RouteStatus.ASSIGNED, routeNotStarted.getStatus());
        // Deve exibir: "Iniciar rota"

        // Cenário 2: Rota em andamento (IN_PROGRESS)
        Route routeInProgress = new Route("2", "ROTA-02", "driver_1", RouteStatus.IN_PROGRESS, 30, waypoints1);
        assertFalse(routeInProgress.areAllWaypointsHandled());
        assertEquals(RouteStatus.IN_PROGRESS, routeInProgress.getStatus());
        // Deve exibir: "Continuar rota", retomando do ponto pendente:
        assertEquals(101L, routeInProgress.getNextPendingWaypoint().getDeliveryId());

        // Cenário 3: Todas as paradas tratadas
        waypoints1.get(0).setStatus(WaypointStatus.DELIVERED);
        assertTrue(routeInProgress.areAllWaypointsHandled());
        // Deve exibir: "Concluir rota"
    }
}
