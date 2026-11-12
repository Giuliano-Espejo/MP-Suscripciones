package com.testsusmp.subs.domain;

import java.util.Collection;

public final class TierPolicy {

    private TierPolicy() {
    }

    public static void assertTierAvailable(Tier tier, Collection<Tier> tiersOfOtherActivePlans) {
        if (tiersOfOtherActivePlans.contains(tier)) {
            throw new BusinessException("Ya existe un plan activo para el nivel " + tier.name());
        }
    }

    public static PlanChange direction(Tier from, Tier to) {
        int diff = Integer.compare(to.level(), from.level());
        if (diff > 0) {
            return PlanChange.UPGRADE;
        }
        return diff < 0 ? PlanChange.DOWNGRADE : PlanChange.SAME;
    }
}
