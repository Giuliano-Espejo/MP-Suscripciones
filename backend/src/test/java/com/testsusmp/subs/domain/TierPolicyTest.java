package com.testsusmp.subs.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TierPolicyTest {

    @Test
    void thereAreExactlyThreeMembershipLevels() {
        assertThat(Tier.values()).extracting(Tier::level).containsExactly(1, 2, 3);
    }

    @Test
    void tierAlreadyUsedByAnotherActivePlanIsRejected() {
        assertThatThrownBy(() -> TierPolicy.assertTierAvailable(Tier.PLUS, List.of(Tier.BASICO, Tier.PLUS)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("PLUS");
    }

    @Test
    void freeTierIsAccepted() {
        TierPolicy.assertTierAvailable(Tier.PREMIUM, List.of(Tier.BASICO, Tier.PLUS));
    }

    @Test
    void movingToHigherLevelIsUpgrade() {
        assertThat(TierPolicy.direction(Tier.BASICO, Tier.PREMIUM)).isEqualTo(PlanChange.UPGRADE);
    }

    @Test
    void movingToLowerLevelIsDowngrade() {
        assertThat(TierPolicy.direction(Tier.PREMIUM, Tier.PLUS)).isEqualTo(PlanChange.DOWNGRADE);
    }

    @Test
    void sameLevelIsNoChange() {
        assertThat(TierPolicy.direction(Tier.PLUS, Tier.PLUS)).isEqualTo(PlanChange.SAME);
    }
}
