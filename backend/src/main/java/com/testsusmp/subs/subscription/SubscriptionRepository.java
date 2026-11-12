package com.testsusmp.subs.subscription;

import com.testsusmp.subs.domain.SubscriptionStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    @EntityGraph(attributePaths = {"member", "plan"})
    List<Subscription> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"member", "plan"})
    List<Subscription> findByStatusOrderByCreatedAtDesc(SubscriptionStatus status);

    @EntityGraph(attributePaths = {"member", "plan"})
    List<Subscription> findByStatusOrderByMemberFullNameAsc(SubscriptionStatus status);

    Optional<Subscription> findByGatewayId(String gatewayId);

    long countByPlanIdAndStatus(Long planId, SubscriptionStatus status);

    long countByStatus(SubscriptionStatus status);

    @Query("select count(s) > 0 from Subscription s where s.member.id = :memberId and s.status <> com.testsusmp.subs.domain.SubscriptionStatus.CANCELLED")
    boolean memberHasLiveSubscription(Long memberId);

    @Query("select s from Subscription s where s.gatewayId is not null and s.status <> com.testsusmp.subs.domain.SubscriptionStatus.CANCELLED")
    List<Subscription> findLiveLinkedToGateway();

    @Query("select s from Subscription s where s.scheduledCancelDate <= :today and s.status <> com.testsusmp.subs.domain.SubscriptionStatus.CANCELLED")
    List<Subscription> findDueCancellations(LocalDate today);

    @Query("select s from Subscription s where s.promoActive = true and s.promoEndsOn <= :today and s.status <> com.testsusmp.subs.domain.SubscriptionStatus.CANCELLED")
    List<Subscription> findExpiredPromotions(LocalDate today);
}
