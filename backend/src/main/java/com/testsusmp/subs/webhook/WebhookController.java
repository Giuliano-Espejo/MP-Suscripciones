package com.testsusmp.subs.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.testsusmp.subs.common.MercadoPagoProperties;
import com.testsusmp.subs.plan.PlanService;
import com.testsusmp.subs.subscription.SubscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Notificaciones de Mercado Pago. Configurar en Tus integraciones > Webhooks la URL
 * https://TU-DOMINIO/api/webhooks/mercadopago con el topico "Planes y suscripciones".
 */
@RestController
@RequestMapping("/api/webhooks/mercadopago")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    private final SubscriptionService subscriptions;
    private final PlanService plans;
    private final MercadoPagoProperties props;

    public WebhookController(SubscriptionService subscriptions, PlanService plans, MercadoPagoProperties props) {
        this.subscriptions = subscriptions;
        this.plans = plans;
        this.props = props;
    }

    @PostMapping
    public ResponseEntity<Void> receive(@RequestParam(name = "type", required = false) String typeParam,
                                        @RequestParam(name = "topic", required = false) String topicParam,
                                        @RequestParam(name = "data.id", required = false) String dataIdParam,
                                        @RequestHeader(name = "x-signature", required = false) String signature,
                                        @RequestHeader(name = "x-request-id", required = false) String requestId,
                                        @RequestBody(required = false) JsonNode body) {
        String type = firstNonBlank(typeParam, topicParam, text(body, "type"));
        String dataId = firstNonBlank(dataIdParam, body == null ? null : text(body.path("data"), "id"));

        if (props.hasWebhookSecret()
                && !WebhookSignatureValidator.isValid(signature, requestId, dataId, props.webhookSecret())) {
            log.warn("Webhook rechazado: firma invalida (type={}, data.id={})", type, dataId);
            return ResponseEntity.status(401).build();
        }
        if ("subscription_preapproval".equals(type) && dataId != null) {
            subscriptions.syncFromGateway(dataId);
        } else if ("subscription_preapproval_plan".equals(type)) {
            plans.syncFromGateway();
        } else {
            log.debug("Webhook ignorado: type={}, data.id={}", type, dataId);
        }
        return ResponseEntity.ok().build();
    }

    private static String text(JsonNode node, String field) {
        if (node == null || !node.hasNonNull(field)) {
            return null;
        }
        return node.get(field).asText();
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }
}
