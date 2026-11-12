package com.testsusmp.subs.domain;

import java.util.Locale;

/** Estado local de la suscripcion, mapeado a los estados de preapproval de Mercado Pago. */
public enum SubscriptionStatus {
    PENDING("pending"),
    ACTIVE("authorized"),
    PAUSED("paused"),
    CANCELLED("cancelled");

    private final String mpValue;

    SubscriptionStatus(String mpValue) {
        this.mpValue = mpValue;
    }

    public String toMercadoPago() {
        return mpValue;
    }

    public static SubscriptionStatus fromMercadoPago(String status) {
        if (status == null) {
            return PENDING;
        }
        return switch (status.toLowerCase(Locale.ROOT)) {
            case "authorized" -> ACTIVE;
            case "paused" -> PAUSED;
            case "cancelled", "canceled" -> CANCELLED;
            default -> PENDING;
        };
    }
}
