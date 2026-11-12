package com.testsusmp.subs.gateway;

/** Vista de un preapproval de Mercado Pago. status es el valor crudo de MP (pending, authorized, ...). */
public record GatewaySubscription(String id, String initPoint, String status, String externalReference,
                                  String payerEmail, String planId) {
}
