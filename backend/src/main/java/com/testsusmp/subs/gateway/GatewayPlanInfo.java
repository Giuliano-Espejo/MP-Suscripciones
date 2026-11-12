package com.testsusmp.subs.gateway;

import java.math.BigDecimal;

/** Plan tal como existe en Mercado Pago (fuente de verdad de nombre, precio y estado). */
public record GatewayPlanInfo(String id, String reason, String status, BigDecimal amount, String currency,
                              String initPoint) {
}
