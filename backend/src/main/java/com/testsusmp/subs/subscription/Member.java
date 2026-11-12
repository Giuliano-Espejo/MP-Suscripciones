package com.testsusmp.subs.subscription;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Socio: la persona que paga la membresia. Se identifica por email (el payer_email de Mercado Pago). */
@Entity
@Table(name = "members")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fullName;
    private String email;
    private Instant createdAt;

    protected Member() {
    }

    public Member(String fullName, String email, Instant now) {
        this.fullName = fullName;
        this.email = email;
        this.createdAt = now;
    }

    public void rename(String fullName) {
        this.fullName = fullName;
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }
}
