package com.testsusmp.subs.subscription;

import com.testsusmp.subs.common.MercadoPagoProperties;
import com.testsusmp.subs.common.NotFoundException;
import com.testsusmp.subs.domain.BillingPeriod;
import com.testsusmp.subs.domain.BusinessException;
import com.testsusmp.subs.domain.CancellationDecision;
import com.testsusmp.subs.domain.HoursCalculator;
import com.testsusmp.subs.domain.PermanencePolicy;
import com.testsusmp.subs.domain.PlanChange;
import com.testsusmp.subs.domain.PriceQuote;
import com.testsusmp.subs.domain.PricingCalculator;
import com.testsusmp.subs.domain.SubscriptionStatus;
import com.testsusmp.subs.domain.TierPolicy;
import com.testsusmp.subs.gateway.GatewaySubscription;
import com.testsusmp.subs.gateway.PaymentGateway;
import com.testsusmp.subs.gateway.SubscriptionSpec;
import com.testsusmp.subs.plan.Plan;
import com.testsusmp.subs.plan.PlanRepository;
import com.testsusmp.subs.plan.PlanService;
import com.testsusmp.subs.promotion.Promotion;
import com.testsusmp.subs.promotion.PromotionRepository;
import com.testsusmp.subs.subscription.SubscriptionDtos.CancelResult;
import com.testsusmp.subs.subscription.SubscriptionDtos.ChangePlanResult;
import com.testsusmp.subs.subscription.SubscriptionDtos.ConsumptionItem;
import com.testsusmp.subs.subscription.SubscriptionDtos.ConsumptionRequest;
import com.testsusmp.subs.subscription.SubscriptionDtos.ConsumptionSummary;
import com.testsusmp.subs.subscription.SubscriptionDtos.CreateSubscriptionRequest;
import com.testsusmp.subs.subscription.SubscriptionDtos.JobsResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class SubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);
    private static final String EXTERNAL_REF_PREFIX = "SUB-";

    private final SubscriptionRepository subscriptions;
    private final MemberRepository members;
    private final ConsumptionRepository consumptions;
    private final PlanService planService;
    private final PlanRepository plans;
    private final PromotionRepository promotions;
    private final PaymentGateway gateway;
    private final MercadoPagoProperties props;
    private final Clock clock;

    public SubscriptionService(SubscriptionRepository subscriptions, MemberRepository members,
                               ConsumptionRepository consumptions, PlanService planService, PlanRepository plans,
                               PromotionRepository promotions, PaymentGateway gateway, MercadoPagoProperties props,
                               Clock clock) {
        this.subscriptions = subscriptions;
        this.members = members;
        this.consumptions = consumptions;
        this.planService = planService;
        this.plans = plans;
        this.promotions = promotions;
        this.gateway = gateway;
        this.props = props;
        this.clock = clock;
    }

    // ---------------------------------------------------------------- consultas

    @Transactional(readOnly = true)
    public List<SubscriptionView> list(SubscriptionStatus status) {
        List<Subscription> result = status == null
                ? subscriptions.findAllByOrderByCreatedAtDesc()
                : subscriptions.findByStatusOrderByCreatedAtDesc(status);
        return result.stream().map(this::view).toList();
    }

    @Transactional(readOnly = true)
    public List<SubscriptionView> activeMembers() {
        return subscriptions.findByStatusOrderByMemberFullNameAsc(SubscriptionStatus.ACTIVE).stream()
                .map(this::view).toList();
    }

    @Transactional(readOnly = true)
    public SubscriptionView get(long id) {
        return view(find(id));
    }

    // ---------------------------------------------------------------- alta

    @Transactional
    public SubscriptionView create(CreateSubscriptionRequest req) {
        Plan plan = planService.find(req.planId());
        if (!plan.isActive()) {
            throw new BusinessException("El plan " + plan.getName() + " no acepta nuevas suscripciones");
        }
        Member member = findOrCreateMember(req.email(), req.fullName().trim());
        if (member.getId() != null && subscriptions.memberHasLiveSubscription(member.getId())) {
            throw new BusinessException("El socio " + member.getEmail() + " ya tiene una suscripcion vigente");
        }
        Subscription sub = newSubscription(member, plan);

        GatewaySubscription gs = gateway.createSubscription(new SubscriptionSpec(plan.getName(),
                sub.externalReference(), member.getEmail(), sub.getAmount(), plan.getCurrency(), props.backUrl()));
        sub.linkGateway(gs.id(), gs.initPoint());
        return view(sub);
    }

    // ---------------------------------------------------------------- sincronizacion con Mercado Pago

    /**
     * Trae el preapproval de la pasarela y actualiza el estado local. Si el preapproval no existe localmente
     * (el socio se suscribio desde el link publico del plan), se da de alta el socio y la suscripcion.
     */
    @Transactional
    public Optional<SubscriptionView> syncFromGateway(String preapprovalId) {
        GatewaySubscription gs = gateway.getSubscription(preapprovalId);
        Optional<Subscription> local = subscriptions.findByGatewayId(gs.id())
                .or(() -> findByExternalReference(gs.externalReference()))
                .or(() -> registerFromPlanLink(gs));
        if (local.isEmpty()) {
            log.warn("Preapproval {} sin suscripcion ni plan asociado; se ignora", gs.id());
            return Optional.empty();
        }
        Subscription sub = local.get();
        sub.linkGateway(gs.id(), gs.initPoint());
        sub.applyStatus(SubscriptionStatus.fromMercadoPago(gs.status()), today());
        return Optional.of(view(sub));
    }

    public record SyncResult(int checked, int changed) {
    }

    /** Consulta en Mercado Pago el estado de todas las suscripciones vigentes. Sirve sin webhook publico. */
    @Transactional
    public SyncResult syncAllFromGateway() {
        int checked = 0;
        int changed = 0;
        for (Subscription sub : subscriptions.findLiveLinkedToGateway()) {
            SubscriptionStatus before = sub.getStatus();
            GatewaySubscription gs = gateway.getSubscription(sub.getGatewayId());
            sub.applyStatus(SubscriptionStatus.fromMercadoPago(gs.status()), today());
            checked++;
            if (sub.getStatus() != before) {
                changed++;
            }
        }
        return new SyncResult(checked, changed);
    }

    private Optional<Subscription> findByExternalReference(String externalReference) {
        if (externalReference == null || !externalReference.startsWith(EXTERNAL_REF_PREFIX)) {
            return Optional.empty();
        }
        try {
            return subscriptions.findById(Long.parseLong(externalReference.substring(EXTERNAL_REF_PREFIX.length())));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private Optional<Subscription> registerFromPlanLink(GatewaySubscription gs) {
        if (gs.planId() == null || gs.payerEmail() == null) {
            return Optional.empty();
        }
        return plans.findByGatewayPlanId(gs.planId()).map(plan -> {
            String email = gs.payerEmail().toLowerCase(Locale.ROOT);
            Member member = findOrCreateMember(email, email.substring(0, Math.max(1, email.indexOf('@'))));
            Subscription sub = newSubscription(member, plan);
            sub.linkGateway(gs.id(), gs.initPoint());
            if (sub.isPromoActive()) {
                // El checkout del plan cobra precio de lista: se ajusta el preapproval al precio promocional.
                gateway.updateSubscriptionAmount(gs.id(), sub.getAmount(), plan.getCurrency());
            }
            log.info("Alta desde link de plan: {} -> {}", email, plan.getName());
            return sub;
        });
    }

    // ---------------------------------------------------------------- gestion

    @Transactional
    public SubscriptionView pause(long id) {
        Subscription sub = find(id);
        sub.requireStatus(SubscriptionStatus.ACTIVE, "pausar");
        pushStatus(sub, SubscriptionStatus.PAUSED);
        return view(sub);
    }

    @Transactional
    public SubscriptionView resume(long id) {
        Subscription sub = find(id);
        sub.requireStatus(SubscriptionStatus.PAUSED, "reactivar");
        pushStatus(sub, SubscriptionStatus.ACTIVE);
        return view(sub);
    }

    /** Baja respetando la permanencia minima: antes de cumplirla, la baja queda programada. */
    @Transactional
    public CancelResult requestCancellation(long id) {
        Subscription sub = find(id);
        if (!sub.isLive()) {
            throw new BusinessException("La suscripcion ya esta dada de baja");
        }
        LocalDate today = today();
        CancellationDecision decision = PermanencePolicy.decide(sub.getStartDate(),
                sub.getPlan().getMinPermanenceMonths(), today);
        if (decision.immediate()) {
            pushStatus(sub, SubscriptionStatus.CANCELLED);
        } else {
            sub.scheduleCancellation(decision.effectiveDate());
        }
        return new CancelResult(decision.immediate(), decision.effectiveDate(), view(sub));
    }

    @Transactional
    public SubscriptionView undoCancellation(long id) {
        Subscription sub = find(id);
        sub.undoScheduledCancellation();
        return view(sub);
    }

    /** Upgrade/downgrade entre los tres niveles. Mantiene fecha de alta (la permanencia no se reinicia). */
    @Transactional
    public ChangePlanResult changePlan(long id, long newPlanId) {
        Subscription sub = find(id);
        if (sub.getStatus() != SubscriptionStatus.ACTIVE && sub.getStatus() != SubscriptionStatus.PAUSED) {
            throw new BusinessException("Solo se puede cambiar de plan una suscripcion activa o pausada");
        }
        Plan newPlan = planService.find(newPlanId);
        if (!newPlan.isActive()) {
            throw new BusinessException("El plan " + newPlan.getName() + " no esta activo");
        }
        PlanChange change = newPlan.getId().equals(sub.getPlan().getId())
                ? PlanChange.SAME
                : TierPolicy.direction(sub.getPlan().getTier(), newPlan.getTier());
        if (change == PlanChange.SAME) {
            throw new BusinessException("La suscripcion ya esta en ese nivel");
        }
        BigDecimal newAmount = sub.isPromoActive() && sub.getPromotion() != null
                ? PricingCalculator.applyDiscount(newPlan.getPrice(), sub.getPromotion().getDiscountPercent())
                : newPlan.getPrice();
        if (sub.getGatewayId() != null) {
            gateway.updateSubscriptionAmount(sub.getGatewayId(), newAmount, newPlan.getCurrency());
        }
        sub.changePlan(newPlan, newAmount);
        return new ChangePlanResult(change, view(sub));
    }

    // ---------------------------------------------------------------- consumos y horas

    @Transactional
    public ConsumptionSummary addConsumption(long id, ConsumptionRequest req) {
        Subscription sub = find(id);
        sub.requireStatus(SubscriptionStatus.ACTIVE, "registrar consumos en");
        LocalDate date = req.date() != null ? req.date() : today();
        BillingPeriod period = HoursCalculator.currentPeriod(sub.getStartDate(), today());
        if (date.isBefore(period.from()) || !date.isBefore(period.toExclusive()) || date.isAfter(today())) {
            throw new BusinessException("La fecha del consumo debe estar dentro del periodo actual ("
                    + period.from() + " a " + today() + ")");
        }
        BigDecimal used = consumptions.sumHours(sub.getId(), period.from(), period.toExclusive());
        HoursCalculator.assertCanConsume(sub.getPlan().getMonthlyHours(), used, req.hours());
        consumptions.save(new Consumption(sub, date, req.hours(), req.description(), clock.instant()));
        return summary(sub);
    }

    @Transactional(readOnly = true)
    public ConsumptionSummary consumptions(long id) {
        return summary(find(id));
    }

    private ConsumptionSummary summary(Subscription sub) {
        Plan plan = sub.getPlan();
        if (sub.getStartDate() == null) {
            return new ConsumptionSummary(sub.getId(), null, null, plan.getMonthlyHours(), BigDecimal.ZERO,
                    BigDecimal.valueOf(plan.getMonthlyHours()), List.copyOf(plan.getBenefits()), List.of());
        }
        BillingPeriod period = HoursCalculator.currentPeriod(sub.getStartDate(), today());
        List<ConsumptionItem> items = consumptions.findInPeriod(sub.getId(), period.from(), period.toExclusive())
                .stream()
                .map(c -> new ConsumptionItem(c.getId(), c.getConsumedOn(), c.getHours(), c.getDescription()))
                .toList();
        BigDecimal used = items.stream().map(ConsumptionItem::hours).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ConsumptionSummary(sub.getId(), period.from(), period.toExclusive(), plan.getMonthlyHours(), used,
                HoursCalculator.available(plan.getMonthlyHours(), used), List.copyOf(plan.getBenefits()), items);
    }

    // ---------------------------------------------------------------- jobs diarios

    /** Ejecuta bajas programadas vencidas y devuelve al precio de lista las promos terminadas. */
    @Transactional
    public JobsResult runDailyJobs() {
        LocalDate today = today();
        int cancelled = 0;
        for (Subscription sub : subscriptions.findDueCancellations(today)) {
            pushStatus(sub, SubscriptionStatus.CANCELLED);
            cancelled++;
        }
        int promosExpired = 0;
        for (Subscription sub : subscriptions.findExpiredPromotions(today)) {
            if (sub.getGatewayId() != null) {
                gateway.updateSubscriptionAmount(sub.getGatewayId(), sub.getPlan().getPrice(), sub.getPlan().getCurrency());
            }
            sub.endPromotion();
            promosExpired++;
        }
        if (cancelled + promosExpired > 0) {
            log.info("Jobs diarios {}: {} bajas ejecutadas, {} promociones finalizadas", today, cancelled, promosExpired);
        }
        return new JobsResult(cancelled, promosExpired);
    }

    // ---------------------------------------------------------------- helpers

    private Subscription newSubscription(Member member, Plan plan) {
        Promotion promo = promotions.findCurrent(today()).orElse(null);
        PriceQuote quote = PricingCalculator.quote(plan.getPrice(), promo == null ? null : promo.terms(), today());
        Subscription sub = new Subscription(member, plan, quote.amount(), quote.promoApplied() ? promo : null,
                clock.instant());
        return subscriptions.save(sub);
    }

    private Member findOrCreateMember(String email, String fullName) {
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        return members.findByEmailIgnoreCase(normalized)
                .orElseGet(() -> members.save(new Member(fullName, normalized, clock.instant())));
    }

    private void pushStatus(Subscription sub, SubscriptionStatus status) {
        if (sub.getGatewayId() != null) {
            gateway.updateSubscriptionStatus(sub.getGatewayId(), status);
        }
        sub.applyStatus(status, today());
    }

    private Subscription find(long id) {
        return subscriptions.findById(id).orElseThrow(() -> new NotFoundException("Suscripcion " + id + " inexistente"));
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    private SubscriptionView view(Subscription s) {
        Plan plan = s.getPlan();
        Member member = s.getMember();
        BigDecimal used = BigDecimal.ZERO;
        if (s.getStartDate() != null && s.isLive()) {
            BillingPeriod period = HoursCalculator.currentPeriod(s.getStartDate(), today());
            used = consumptions.sumHours(s.getId(), period.from(), period.toExclusive());
        }
        boolean permanenceFulfilled = s.getMinEndDate() != null && !today().isBefore(s.getMinEndDate());
        return new SubscriptionView(s.getId(), member.getId(), member.getFullName(), member.getEmail(),
                plan.getId(), plan.getName(), plan.getTier(), plan.getTier().level(), s.getStatus(), s.getAmount(),
                plan.getPrice(), s.isPromoActive(), s.getPromotion() != null ? s.getPromotion().getName() : null,
                s.getPromoEndsOn(), s.getStartDate(), s.getMinEndDate(), permanenceFulfilled,
                s.getScheduledCancelDate(), s.getCancelledAt(), s.getGatewayId(), s.getInitPoint(),
                plan.getMonthlyHours(), used, HoursCalculator.available(plan.getMonthlyHours(), used), s.getCreatedAt());
    }
}
