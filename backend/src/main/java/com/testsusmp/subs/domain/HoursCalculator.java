package com.testsusmp.subs.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Horas incluidas por periodo mensual. El periodo arranca en el "mesiversario" del alta. */
public final class HoursCalculator {

    private HoursCalculator() {
    }

    public static BillingPeriod currentPeriod(LocalDate start, LocalDate today) {
        long months = ChronoUnit.MONTHS.between(start, today);
        LocalDate from = start.plusMonths(months);
        if (from.isAfter(today)) {
            months--;
            from = start.plusMonths(months);
        }
        return new BillingPeriod(from, start.plusMonths(months + 1));
    }

    public static BigDecimal available(int monthlyHours, BigDecimal usedInPeriod) {
        BigDecimal remaining = BigDecimal.valueOf(monthlyHours).subtract(usedInPeriod);
        return remaining.signum() < 0 ? BigDecimal.ZERO : remaining;
    }

    public static void assertCanConsume(int monthlyHours, BigDecimal usedInPeriod, BigDecimal requested) {
        if (requested == null || requested.signum() <= 0) {
            throw new BusinessException("Las horas a consumir deben ser mayores a 0");
        }
        BigDecimal available = available(monthlyHours, usedInPeriod);
        if (requested.compareTo(available) > 0) {
            throw new BusinessException("Horas insuficientes: disponibles " + available.stripTrailingZeros().toPlainString()
                    + ", solicitadas " + requested.stripTrailingZeros().toPlainString());
        }
    }
}
