package com.testsusmp.subs.domain;

import java.time.LocalDate;

/**
 * Permanencia minima: una suscripcion activada no puede darse de baja antes de cumplir
 * N meses (3 por defecto). Si se pide antes, la baja queda programada para esa fecha.
 */
public final class PermanencePolicy {

    public static final int DEFAULT_MIN_MONTHS = 3;

    private PermanencePolicy() {
    }

    public static LocalDate minimumEndDate(LocalDate start, int minMonths) {
        return start.plusMonths(minMonths);
    }

    public static CancellationDecision decide(LocalDate start, int minMonths, LocalDate today) {
        if (start == null) {
            return new CancellationDecision(true, today);
        }
        LocalDate minEnd = minimumEndDate(start, minMonths);
        if (today.isBefore(minEnd)) {
            return new CancellationDecision(false, minEnd);
        }
        return new CancellationDecision(true, today);
    }
}
