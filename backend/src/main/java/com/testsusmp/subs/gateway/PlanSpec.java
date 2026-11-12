package com.testsusmp.subs.gateway;

import java.math.BigDecimal;

public record PlanSpec(String reason, BigDecimal amount, String currency, int frequencyMonths, String backUrl) {
}
