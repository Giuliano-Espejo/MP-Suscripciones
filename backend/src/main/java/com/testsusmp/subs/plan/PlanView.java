package com.testsusmp.subs.plan;

import com.testsusmp.subs.domain.Tier;

import java.math.BigDecimal;
import java.util.List;

public record PlanView(Long id, String name, String description, Tier tier, int tierLevel, BigDecimal price,
                       String currency, int monthlyHours, int minPermanenceMonths, List<String> benefits,
                       boolean active, String gatewayPlanId, String gatewayInitPoint, long activeSubscribers) {

    static PlanView from(Plan p, long activeSubscribers) {
        return new PlanView(p.getId(), p.getName(), p.getDescription(), p.getTier(), p.getTier().level(),
                p.getPrice(), p.getCurrency(), p.getMonthlyHours(), p.getMinPermanenceMonths(),
                List.copyOf(p.getBenefits()), p.isActive(), p.getGatewayPlanId(), p.getGatewayInitPoint(),
                activeSubscribers);
    }
}
