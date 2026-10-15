package com.orbi.orbi_app.model;

public enum WaypointStatus {
    PENDING("Pendente"),
    IN_TRANSIT("Em trânsito"),
    COMPLETED("Concluído"),
    SKIPPED("Pulado"),
    CANCELLED("Cancelado");

    private final String displayName;

    WaypointStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
