package com.testsusmp.subs.promotion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    List<Promotion> findAllByOrderByValidFromDesc();

    Optional<Promotion> findFirstByActiveTrueAndValidFromLessThanEqualAndValidToGreaterThanEqualOrderByDiscountPercentDesc(
            LocalDate from, LocalDate to);

    default Optional<Promotion> findCurrent(LocalDate today) {
        return findFirstByActiveTrueAndValidFromLessThanEqualAndValidToGreaterThanEqualOrderByDiscountPercentDesc(today, today);
    }
}
