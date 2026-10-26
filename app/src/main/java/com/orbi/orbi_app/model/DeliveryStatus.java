package com.orbi.orbi_app.model;

public enum DeliveryStatus {
    REQUESTED("Solicitada"),
    ACCEPTED("Aceita"),
    AT_PICKUP("No ponto de coleta"),
    IN_TRANSIT("Em trânsito"),
    DELIVERED("Entregue"),
    CANCELLED("Cancelada");

    private final String displayName;

    DeliveryStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isTerminal() {
        return this == DELIVERED || this == CANCELLED;
    }

    public boolean isAtPickup() {
        return this == REQUESTED || this == ACCEPTED || this == AT_PICKUP;
    }

    public boolean isInTransit() {
        return this == IN_TRANSIT;
    }
}
