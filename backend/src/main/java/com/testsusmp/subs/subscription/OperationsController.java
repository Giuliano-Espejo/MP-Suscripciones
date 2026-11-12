package com.testsusmp.subs.subscription;

import com.testsusmp.subs.common.NotFoundException;
import com.testsusmp.subs.domain.BusinessException;
import com.testsusmp.subs.gateway.FakePaymentGateway;
import com.testsusmp.subs.gateway.PaymentGateway;
import com.testsusmp.subs.subscription.SubscriptionDtos.JobsResult;
import com.testsusmp.subs.subscription.SubscriptionDtos.SimulateRequest;
import com.testsusmp.subs.plan.PlanService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Jobs diarios (cron + disparo manual) y simulador de pagos del modo demo. */
@RestController
@RequestMapping("/api")
public class OperationsController {

    private static final Logger log = LoggerFactory.getLogger(OperationsController.class);

    private final SubscriptionService service;
    private final PlanService plans;
    private final PaymentGateway gateway;

    public OperationsController(SubscriptionService service, PlanService plans, PaymentGateway gateway) {
        this.service = service;
        this.plans = plans;
        this.gateway = gateway;
    }

    @Scheduled(cron = "${app.jobs.cron}", zone = "${app.timezone}")
    public void scheduledJobs() {
        service.runDailyJobs();
    }

    /** Con Mercado Pago real: al arrancar y cada pocos minutos se trae el estado de planes y suscripciones. */
    @EventListener(ApplicationReadyEvent.class)
    @Scheduled(cron = "${app.jobs.sync-cron}", zone = "${app.timezone}")
    public void syncWithMercadoPago() {
        if (!"mercadopago".equals(gateway.mode())) {
            return;
        }
        try {
            PlanService.PlanSyncResult p = plans.syncFromGateway();
            SubscriptionService.SyncResult s = service.syncAllFromGateway();
            log.info("Sync Mercado Pago: planes {}, suscripciones {}", p, s);
        } catch (RuntimeException e) {
            log.warn("No se pudo sincronizar con Mercado Pago: {}", e.getMessage());
        }
    }

    @PostMapping("/admin/jobs/run")
    public JobsResult runJobs() {
        return service.runDailyJobs();
    }

    /** Solo modo demo: simula que el socio completo el checkout (authorized) o lo rechazo (cancelled). */
    @PostMapping("/demo/subscriptions/{id}/simulate")
    public SubscriptionView simulate(@PathVariable long id, @Valid @RequestBody SimulateRequest req) {
        if (!(gateway instanceof FakePaymentGateway fake)) {
            throw new BusinessException("El simulador solo esta disponible en modo demo");
        }
        String gatewayId = service.get(id).gatewayId();
        fake.simulate(gatewayId, req.status());
        return service.syncFromGateway(gatewayId)
                .orElseThrow(() -> new NotFoundException("Suscripcion " + id + " inexistente"));
    }
}
