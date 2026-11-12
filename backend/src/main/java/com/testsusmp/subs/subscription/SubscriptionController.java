package com.testsusmp.subs.subscription;

import com.testsusmp.subs.domain.SubscriptionStatus;
import com.testsusmp.subs.subscription.SubscriptionDtos.CancelResult;
import com.testsusmp.subs.subscription.SubscriptionDtos.ChangePlanRequest;
import com.testsusmp.subs.subscription.SubscriptionDtos.ChangePlanResult;
import com.testsusmp.subs.subscription.SubscriptionDtos.ConsumptionRequest;
import com.testsusmp.subs.subscription.SubscriptionDtos.ConsumptionSummary;
import com.testsusmp.subs.subscription.SubscriptionDtos.CreateSubscriptionRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class SubscriptionController {

    private final SubscriptionService service;

    public SubscriptionController(SubscriptionService service) {
        this.service = service;
    }

    @GetMapping("/subscriptions")
    public List<SubscriptionView> list(@RequestParam(required = false) SubscriptionStatus status) {
        return service.list(status);
    }

    @GetMapping("/subscriptions/{id}")
    public SubscriptionView get(@PathVariable long id) {
        return service.get(id);
    }

    @PostMapping("/subscriptions")
    @ResponseStatus(HttpStatus.CREATED)
    public SubscriptionView create(@Valid @RequestBody CreateSubscriptionRequest req) {
        return service.create(req);
    }

    /** Trae de Mercado Pago el estado actual de las suscripciones vigentes. */
    @PostMapping("/subscriptions/sync")
    public SubscriptionService.SyncResult sync() {
        return service.syncAllFromGateway();
    }

    @PostMapping("/subscriptions/{id}/pause")
    public SubscriptionView pause(@PathVariable long id) {
        return service.pause(id);
    }

    @PostMapping("/subscriptions/{id}/resume")
    public SubscriptionView resume(@PathVariable long id) {
        return service.resume(id);
    }

    @PostMapping("/subscriptions/{id}/cancel")
    public CancelResult cancel(@PathVariable long id) {
        return service.requestCancellation(id);
    }

    @DeleteMapping("/subscriptions/{id}/cancel")
    public SubscriptionView undoCancel(@PathVariable long id) {
        return service.undoCancellation(id);
    }

    @PutMapping("/subscriptions/{id}/plan")
    public ChangePlanResult changePlan(@PathVariable long id, @Valid @RequestBody ChangePlanRequest req) {
        return service.changePlan(id, req.planId());
    }

    @GetMapping("/subscriptions/{id}/consumptions")
    public ConsumptionSummary consumptions(@PathVariable long id) {
        return service.consumptions(id);
    }

    @PostMapping("/subscriptions/{id}/consumptions")
    @ResponseStatus(HttpStatus.CREATED)
    public ConsumptionSummary addConsumption(@PathVariable long id, @Valid @RequestBody ConsumptionRequest req) {
        return service.addConsumption(id, req);
    }

    /** Listado de socios activos. */
    @GetMapping("/members/active")
    public List<SubscriptionView> activeMembers() {
        return service.activeMembers();
    }
}
