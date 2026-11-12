package com.testsusmp.subs.gateway;

import com.testsusmp.subs.domain.SubscriptionStatus;

import java.math.BigDecimal;

/** Puerto hacia la pasarela de pagos (Mercado Pago real o modo demo). */
public interface PaymentGateway {

    /** "mercadopago" o "demo". */
    String mode();

    GatewayPlan createPlan(PlanSpec spec);

    void updatePlan(String gatewayPlanId, PlanSpec spec);

    void deactivatePlan(String gatewayPlanId);

    /** Planes activos en la cuenta de Mercado Pago (incluye los creados desde el panel de MP). */
    java.util.List<GatewayPlanInfo> listActivePlans();

    GatewaySubscription createSubscription(SubscriptionSpec spec);

    GatewaySubscription getSubscription(String preapprovalId);

    void updateSubscriptionStatus(String preapprovalId, SubscriptionStatus status);

    void updateSubscriptionAmount(String preapprovalId, BigDecimal amount, String currency);
}
