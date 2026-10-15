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
        this.status = status;
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
            if (w.getStatus() == WaypointStatus.COMPLETED) {
                count++;
            }
        }
        return count;
    }

    public int getRemainingWaypoints() {
        int count = 0;
        for (Waypoint w : waypoints) {
            if (w.getStatus() == WaypointStatus.PENDING || w.getStatus() == WaypointStatus.IN_TRANSIT) {
                count++;
            }
        }
        return count;
    }

    public boolean areAllWaypointsHandled() {
        for (Waypoint w : waypoints) {
            if (w.getStatus() == WaypointStatus.PENDING || w.getStatus() == WaypointStatus.IN_TRANSIT) {
                return false;
            }
        }
        return true;
    }

    public Waypoint getNextPendingWaypoint() {
        for (Waypoint w : waypoints) {
            if (w.getStatus() == WaypointStatus.PENDING || w.getStatus() == WaypointStatus.IN_TRANSIT) {
                return w;
            }
        }
        return null;
    }
}
