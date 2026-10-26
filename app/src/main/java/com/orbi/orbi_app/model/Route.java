package com.orbi.orbi_app.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Route implements Serializable {
    private String id;
    private String code;
    private String driverId;
    private RouteStatus status;
    private int estimatedDurationMinutes;
    private List<Waypoint> waypoints;

    public Route(String id, String code, String driverId, RouteStatus status,
                 int estimatedDurationMinutes, List<Waypoint> waypoints) {
        this.id = id;
        this.code = code;
        this.driverId = driverId;
        this.status = status != null ? status : RouteStatus.ASSIGNED;
        this.estimatedDurationMinutes = estimatedDurationMinutes;
        this.waypoints = waypoints != null ? waypoints : new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public long getNumericId() {
        try {
            return Long.parseLong(id.replaceAll("\\D+", ""));
        } catch (Exception e) {
            return 1L;
        }
    }

    public String getCode() {
        return code;
    }

    public String getDriverId() {
        return driverId;
    }

    public RouteStatus getStatus() {
        return status;
    }

    public void setStatus(RouteStatus status) {
        this.status = status;
    }

    public int getEstimatedDurationMinutes() {
        return estimatedDurationMinutes;
    }

    public List<Waypoint> getWaypoints() {
        return waypoints;
    }

    public void setWaypoints(List<Waypoint> waypoints) {
        this.waypoints = waypoints;
    }

    public int getTotalWaypoints() {
        return waypoints.size();
    }

    public int getCompletedWaypoints() {
        int count = 0;
        for (Waypoint w : waypoints) {
            if (w.isDelivered()) {
                count++;
            }
        }
        return count;
    }

    public int getRemainingWaypoints() {
        int count = 0;
        for (Waypoint w : waypoints) {
            if (!w.isTerminal()) {
                count++;
            }
        }
        return count;
    }

    public boolean areAllWaypointsHandled() {
        if (waypoints == null || waypoints.isEmpty()) return false;
        for (Waypoint w : waypoints) {
            if (!w.isTerminal()) {
                return false;
            }
        }
        return true;
    }

    public Waypoint getNextPendingWaypoint() {
        if (waypoints == null) return null;
        for (Waypoint w : waypoints) {
            if (!w.isTerminal()) {
                return w;
            }
        }
        return null;
    }

    /**
     * Move a parada para o final da fila de pendentes da rota (reordena operacionalmente).
     */
    public boolean postponeWaypoint(long deliveryId) {
        int targetIndex = -1;
        for (int i = 0; i < waypoints.size(); i++) {
            if (waypoints.get(i).getDeliveryId() == deliveryId) {
                targetIndex = i;
                break;
            }
        }

        if (targetIndex >= 0 && targetIndex < waypoints.size()) {
            Waypoint target = waypoints.remove(targetIndex);
            waypoints.add(target);

            // Reordena sequencialmente os números de exibição
            for (int i = 0; i < waypoints.size(); i++) {
                waypoints.get(i).setSequenceOrder(i + 1);
            }
            return true;
        }
        return false;
    }
}
