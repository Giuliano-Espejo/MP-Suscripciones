package com.testsusmp.subs.dashboard;

import com.testsusmp.subs.domain.SubscriptionStatus;
import com.testsusmp.subs.domain.Tier;
import com.testsusmp.subs.gateway.PaymentGateway;
import com.testsusmp.subs.promotion.Promotion;
import com.testsusmp.subs.promotion.PromotionRepository;
import com.testsusmp.subs.subscription.SubscriptionService;
import com.testsusmp.subs.subscription.SubscriptionView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    public record Summary(long activeMembers, long pending, long paused, long cancelled, long scheduledCancellations,
                          long onPromotion, BigDecimal monthlyRevenue, Map<Tier, Long> activeByTier,
                          Promotion currentPromotion, String gatewayMode, LocalDate today) {
    }

    private final SubscriptionService subscriptions;
    private final PromotionRepository promotions;
    private final PaymentGateway gateway;
    private final Clock clock;

    public DashboardController(SubscriptionService subscriptions, PromotionRepository promotions,
                               PaymentGateway gateway, Clock clock) {
        this.subscriptions = subscriptions;
        this.promotions = promotions;
        this.gateway = gateway;
        this.clock = clock;
    }

    @GetMapping("/summary")
    public Summary summary() {
        LocalDate today = LocalDate.now(clock);
        List<SubscriptionView> all = subscriptions.list(null);
        List<SubscriptionView> active = all.stream().filter(s -> s.status() == SubscriptionStatus.ACTIVE).toList();

        Map<Tier, Long> byTier = new EnumMap<>(Tier.class);
        for (Tier t : Tier.values()) {
            byTier.put(t, active.stream().filter(s -> s.tier() == t).count());
        }
        return new Summary(
                active.size(),
                count(all, SubscriptionStatus.PENDING),
                count(all, SubscriptionStatus.PAUSED),
                count(all, SubscriptionStatus.CANCELLED),
                all.stream().filter(s -> s.scheduledCancelDate() != null && s.status() != SubscriptionStatus.CANCELLED).count(),
                active.stream().filter(SubscriptionView::promoActive).count(),
                active.stream().map(SubscriptionView::amount).reduce(BigDecimal.ZERO, BigDecimal::add),
                byTier,
                promotions.findCurrent(today).orElse(null),
                gateway.mode(),
                today);
    }

    private static long count(List<SubscriptionView> all, SubscriptionStatus status) {
        return all.stream().filter(s -> s.status() == status).count();
    }
}
