package com.orbi.orbi_app.model;

public enum WaypointStatus {
    REQUESTED("Solicitada"),
    ACCEPTED("Aceita"),
    AT_PICKUP("No ponto de coleta"),
    PENDING("Pendente"),
    IN_TRANSIT("Em trânsito"),
    COMPLETED("Concluído"),
    DELIVERED("Entregue"),
    SKIPPED("Pulado"),
    CANCELLED("Cancelado");

    private final String displayName;

    WaypointStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == DELIVERED || this == CANCELLED;
    }

    public boolean isAtPickup() {
        return this == AT_PICKUP || this == PENDING || this == REQUESTED || this == ACCEPTED;
    }

    public boolean isInTransit() {
        return this == IN_TRANSIT;
    }
}
