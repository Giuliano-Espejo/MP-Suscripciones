package com.testsusmp.subs.domain;

/** Violacion de una regla de negocio. Se traduce a HTTP 409. */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
