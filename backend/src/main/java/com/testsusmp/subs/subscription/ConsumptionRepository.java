package com.testsusmp.subs.subscription;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ConsumptionRepository extends JpaRepository<Consumption, Long> {

    @Query("""
            select coalesce(sum(c.hours), 0) from Consumption c
            where c.subscription.id = :subscriptionId and c.consumedOn >= :from and c.consumedOn < :to
            """)
    BigDecimal sumHours(Long subscriptionId, LocalDate from, LocalDate to);

    @Query("""
            select c from Consumption c
            where c.subscription.id = :subscriptionId and c.consumedOn >= :from and c.consumedOn < :to
            order by c.consumedOn desc, c.id desc
            """)
    List<Consumption> findInPeriod(Long subscriptionId, LocalDate from, LocalDate to);
}
