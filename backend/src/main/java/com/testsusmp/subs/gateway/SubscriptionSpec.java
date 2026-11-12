package com.testsusmp.subs.gateway;

import java.math.BigDecimal;

public record SubscriptionSpec(String reason, String externalReference, String payerEmail, BigDecimal amount,
                               String currency, String backUrl) {
}
