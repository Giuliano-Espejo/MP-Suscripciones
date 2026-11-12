package com.testsusmp.subs.promotion;

import com.testsusmp.subs.common.NotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Promociones de alta (ej. lanzamiento de noviembre y diciembre). Solo aplican a nuevas suscripciones. */
@RestController
@RequestMapping("/api/promotions")
public class PromotionController {

    private final PromotionRepository repo;

    public PromotionController(PromotionRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<Promotion> list() {
        return repo.findAllByOrderByValidFromDesc();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Promotion create(@Valid @RequestBody PromotionRequest req) {
        return repo.save(new Promotion(req));
    }

    @PutMapping("/{id}")
    @Transactional
    public Promotion update(@PathVariable long id, @Valid @RequestBody PromotionRequest req) {
        Promotion promo = repo.findById(id).orElseThrow(() -> new NotFoundException("Promocion " + id + " inexistente"));
        promo.apply(req);
        return promo;
    }
}
