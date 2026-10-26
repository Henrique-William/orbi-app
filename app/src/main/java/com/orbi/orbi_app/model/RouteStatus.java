package com.orbi.orbi_app.model;

public enum RouteStatus {
    ALL("Todas"),
    PENDING("Pendente"),
    ASSIGNED("Atribuída"),
    IN_PROGRESS("Em andamento"),
    COMPLETED("Concluída"),
    CANCELLED("Cancelada");

    private final String displayName;

    RouteStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isActive() {
        return this == ASSIGNED || this == IN_PROGRESS || this == PENDING;
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED;
    }
}
