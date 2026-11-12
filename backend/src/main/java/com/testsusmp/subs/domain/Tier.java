package com.testsusmp.subs.domain;

/** Los tres niveles de membresia. Cada nivel puede tener a lo sumo un plan activo. */
public enum Tier {
    BASICO(1),
    PLUS(2),
    PREMIUM(3);

    private final int level;

    Tier(int level) {
        this.level = level;
    }

    public int level() {
        return level;
    }
}
