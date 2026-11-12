package com.testsusmp.subs.plan;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlanRepository extends JpaRepository<Plan, Long> {

    List<Plan> findAllByOrderByActiveDescTierAscNameAsc();

    List<Plan> findByActiveTrueOrderByTierAsc();

    Optional<Plan> findByGatewayPlanId(String gatewayPlanId);
}
