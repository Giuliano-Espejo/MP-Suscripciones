package com.testsusmp.subs.gateway;

import com.testsusmp.subs.domain.SubscriptionStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pasarela simulada (modo demo). Guarda planes y preapprovals en memoria y permite simular
 * la aprobacion del pago desde el tablero, sin credenciales de Mercado Pago.
 */
public class FakePaymentGateway implements PaymentGateway {

    private record FakePreapproval(String id, String planId, String email, String externalReference,
                                   String status, BigDecimal amount) {
        FakePreapproval withStatus(String newStatus) {
            return new FakePreapproval(id, planId, email, externalReference, newStatus, amount);
        }

        FakePreapproval withAmount(BigDecimal newAmount) {
            return new FakePreapproval(id, planId, email, externalReference, status, newAmount);
        }
    }

    private final Map<String, GatewayPlanInfo> plans = new ConcurrentHashMap<>();
    private final Map<String, FakePreapproval> preapprovals = new ConcurrentHashMap<>();

    @Override
    public String mode() {
        return "demo";
    }

    @Override
    public GatewayPlan createPlan(PlanSpec spec) {
        String id = "demo-plan-" + UUID.randomUUID();
        String initPoint = "demo://checkout?preapproval_plan_id=" + id;
        plans.put(id, new GatewayPlanInfo(id, spec.reason(), "active", spec.amount(), spec.currency(), initPoint));
        return new GatewayPlan(id, initPoint);
    }

    @Override
    public void updatePlan(String gatewayPlanId, PlanSpec spec) {
        plans.computeIfPresent(gatewayPlanId, (k, p) ->
                new GatewayPlanInfo(k, spec.reason(), p.status(), spec.amount(), spec.currency(), p.initPoint()));
    }

    @Override
    public void deactivatePlan(String gatewayPlanId) {
        plans.computeIfPresent(gatewayPlanId, (k, p) ->
                new GatewayPlanInfo(k, p.reason(), "cancelled", p.amount(), p.currency(), p.initPoint()));
    }

    @Override
    public List<GatewayPlanInfo> listActivePlans() {
        return plans.values().stream().filter(p -> "active".equals(p.status())).toList();
    }

    /** Simula un plan creado directamente desde el panel de Mercado Pago. */
    public void registerExternalPlan(String id, String reason, BigDecimal amount) {
        plans.put(id, new GatewayPlanInfo(id, reason, "active", amount, "ARS", "demo://checkout?preapproval_plan_id=" + id));
    }

    /** Simula una edicion hecha desde el panel de Mercado Pago. */
    public void editExternalPlan(String id, String reason, BigDecimal amount) {
        plans.computeIfPresent(id, (k, p) -> new GatewayPlanInfo(k, reason, p.status(), amount, p.currency(), p.initPoint()));
    }

    @Override
    public GatewaySubscription createSubscription(SubscriptionSpec spec) {
        String id = "demo-pre-" + UUID.randomUUID();
        preapprovals.put(id, new FakePreapproval(id, null, spec.payerEmail(), spec.externalReference(), "pending",
                spec.amount()));
        return toView(preapprovals.get(id));
    }

    @Override
    public GatewaySubscription getSubscription(String preapprovalId) {
        FakePreapproval p = preapprovals.get(preapprovalId);
        if (p == null) {
            throw new GatewayException("Preapproval inexistente: " + preapprovalId, null);
        }
        return toView(p);
    }

    @Override
    public void updateSubscriptionStatus(String preapprovalId, SubscriptionStatus status) {
        preapprovals.computeIfPresent(preapprovalId, (k, p) -> p.withStatus(status.toMercadoPago()));
    }

    @Override
    public void updateSubscriptionAmount(String preapprovalId, BigDecimal amount, String currency) {
        preapprovals.computeIfPresent(preapprovalId, (k, p) -> p.withAmount(amount));
    }

    /** Simula que el pagador completo (o rechazo) el checkout. */
    public void simulate(String preapprovalId, String mpStatus) {
        preapprovals.computeIfPresent(preapprovalId, (k, p) -> p.withStatus(mpStatus));
    }

    /** Simula un alta hecha por el socio desde el link del plan (sin external_reference). */
    public void registerExternalPreapproval(String id, String gatewayPlanId, String email, String mpStatus) {
        preapprovals.put(id, new FakePreapproval(id, gatewayPlanId, email, null, mpStatus, null));
    }

    /** Vacia el estado simulado (usado por los tests). */
    public void reset() {
        plans.clear();
        preapprovals.clear();
    }

    public String statusOf(String preapprovalId) {
        return preapprovals.get(preapprovalId).status();
    }

    public BigDecimal amountOf(String preapprovalId) {
        return preapprovals.get(preapprovalId).amount();
    }

    private static GatewaySubscription toView(FakePreapproval p) {
        return new GatewaySubscription(p.id(), "demo://checkout/" + p.id(), p.status(), p.externalReference(),
                p.email(), p.planId());
    }
}
