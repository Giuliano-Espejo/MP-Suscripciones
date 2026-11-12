package com.testsusmp.subs;

import org.springframework.boot.SpringApplication;

/**
 * Arranque local sin PostgreSQL: {@code ./gradlew bootTestRun}.
 * Usa H2 en memoria (perfil "local") y la pasarela demo si no hay MP_ACCESS_TOKEN.
 */
public class LocalSubsApplication {

    public static void main(String[] args) {
        SpringApplication.from(SubsApplication::main)
                .withAdditionalProfiles("local")
                .run(args);
    }
}
