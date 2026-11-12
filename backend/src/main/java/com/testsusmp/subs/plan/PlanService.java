package com.testsusmp.subs.plan;

import com.testsusmp.subs.common.MercadoPagoProperties;
import com.testsusmp.subs.common.NotFoundException;
import com.testsusmp.subs.domain.SubscriptionStatus;
import com.testsusmp.subs.domain.Tier;
import com.testsusmp.subs.domain.TierPolicy;
import com.testsusmp.subs.gateway.GatewayPlan;
import com.testsusmp.subs.gateway.PaymentGateway;
import com.testsusmp.subs.gateway.PlanSpec;
import com.testsusmp.subs.subscription.SubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.testsusmp.subs.gateway.GatewayPlanInfo;

import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PlanService {

    private final PlanRepository plans;
    private final SubscriptionRepository subscriptions;
    private final PaymentGateway gateway;
    private final MercadoPagoProperties props;
    private final Clock clock;

    public PlanService(PlanRepository plans, SubscriptionRepository subscriptions, PaymentGateway gateway,
                       MercadoPagoProperties props, Clock clock) {
        this.plans = plans;
        this.subscriptions = subscriptions;
        this.gateway = gateway;
        this.props = props;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<PlanView> list(boolean activeOnly) {
        List<Plan> result = activeOnly ? plans.findByActiveTrueOrderByTierAsc() : plans.findAllByOrderByActiveDescTierAscNameAsc();
        return result.stream().map(this::view).toList();
    }

    @Transactional(readOnly = true)
    public PlanView get(long id) {
        return view(find(id));
    }

    @Transactional
    public PlanView create(PlanRequest req) {
        assertTierFree(req.tier(), null);
        Instant now = clock.instant();
        Plan plan = new Plan(props.currency(), now);
        plan.apply(req, now);
        GatewayPlan gp = gateway.createPlan(spec(plan));
        plan.linkGateway(gp.id(), gp.initPoint());
        return view(plans.save(plan));
    }

    @Transactional
    public PlanView update(long id, PlanRequest req) {
        Plan plan = find(id);
        if (plan.isActive()) {
            assertTierFree(req.tier(), plan.getId());
        }
        plan.apply(req, clock.instant());
        if (plan.isActive() && plan.getGatewayPlanId() != null) {
            gateway.updatePlan(plan.getGatewayPlanId(), spec(plan));
        }
        return view(plan);
    }

    /** Baja logica: el plan deja de aceptar altas; las suscripciones existentes siguen vigentes. */
    @Transactional
    public void deactivate(long id) {
        Plan plan = find(id);
        if (!plan.isActive()) {
            return;
        }
        plan.setActive(false, clock.instant());
        if (plan.getGatewayPlanId() != null) {
            gateway.deactivatePlan(plan.getGatewayPlanId());
        }
    }

    @Transactional
    public PlanView reactivate(long id) {
        Plan plan = find(id);
        if (!plan.isActive()) {
            assertTierFree(plan.getTier(), plan.getId());
            plan.setActive(true, clock.instant());
            GatewayPlan gp = gateway.createPlan(spec(plan));
            plan.linkGateway(gp.id(), gp.initPoint());
        }
        return view(plan);
    }

    public record PlanSyncResult(int imported, int updated, int deactivated) {
    }

    /**
     * Trae los planes activos de Mercado Pago (fuente de verdad de nombre, precio y estado):
     * importa los creados desde el panel de MP, aplica ediciones y da de baja los cancelados alla.
     * Nivel, horas, beneficios y permanencia son datos propios del tablero y no se tocan.
     */
    @Transactional
    public PlanSyncResult syncFromGateway() {
        Instant now = clock.instant();
        Map<String, GatewayPlanInfo> remote = new LinkedHashMap<>();
        gateway.listActivePlans().forEach(p -> remote.put(p.id(), p));

        int imported = 0;
        int updated = 0;
        int deactivated = 0;
        for (Plan local : plans.findAll()) {
            if (local.getGatewayPlanId() == null) {
                continue;
            }
            GatewayPlanInfo info = remote.remove(local.getGatewayPlanId());
            if (info == null) {
                if (local.isActive()) {
                    local.setActive(false, now);
                    deactivated++;
                }
            } else if (local.syncFromGateway(info.reason(), info.amount(), now)) {
                updated++;
            }
        }
        for (GatewayPlanInfo info : remote.values()) {
            Set<Tier> taken = plans.findByActiveTrueOrderByTierAsc().stream().map(Plan::getTier)
                    .collect(Collectors.toSet());
            Tier free = Arrays.stream(Tier.values()).filter(t -> !taken.contains(t)).findFirst().orElse(null);
            plans.save(Plan.importFromGateway(info.reason(), info.amount(),
                    info.currency() != null ? info.currency() : props.currency(),
                    free != null ? free : Tier.BASICO, free != null, info.id(), info.initPoint(), now));
            imported++;
        }
        return new PlanSyncResult(imported, updated, deactivated);
    }

    public Plan find(long id) {
        return plans.findById(id).orElseThrow(() -> new NotFoundException("Plan " + id + " inexistente"));
    }

    private void assertTierFree(Tier tier, Long excludePlanId) {
        List<Tier> taken = plans.findByActiveTrueOrderByTierAsc().stream()
                .filter(p -> !Objects.equals(p.getId(), excludePlanId))
                .map(Plan::getTier)
                .toList();
        TierPolicy.assertTierAvailable(tier, taken);
    }

    private PlanSpec spec(Plan plan) {
        return new PlanSpec(plan.getName(), plan.getPrice(), plan.getCurrency(), 1, props.backUrl());
    }

    private PlanView view(Plan plan) {
        long active = plan.getId() == null ? 0 : subscriptions.countByPlanIdAndStatus(plan.getId(), SubscriptionStatus.ACTIVE);
        return PlanView.from(plan, active);
    }
}
