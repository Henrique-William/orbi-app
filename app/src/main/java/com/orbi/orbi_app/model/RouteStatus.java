package com.orbi.orbi_app.model;

public enum RouteStatus {
    ALL("Todas"),
    PENDING("A fazer"),
    DELAYED("Atrasado"),
    IN_PROGRESS("Em andamento"),
    COMPLETED("Concluídas");

    private final String displayName;

    RouteStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
