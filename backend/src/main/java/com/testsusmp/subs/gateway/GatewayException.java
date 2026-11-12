package com.testsusmp.subs.gateway;

/** Error al hablar con la pasarela. Se traduce a HTTP 502. */
public class GatewayException extends RuntimeException {
    public GatewayException(String message, Throwable cause) {
        super(message, cause);
    }
}
