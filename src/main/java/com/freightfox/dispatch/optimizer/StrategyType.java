package com.freightfox.dispatch.optimizer;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/** The dispatch strategies the API can run. */
public enum StrategyType {

    /** Priority-first, capacity-aware, nearest-vehicle greedy with nearest-neighbour routing (default). */
    SMART("Priority first, nearest vehicle with room, stops ordered nearest-next"),

    /** Baseline for comparison: first-come first-served, vehicles take turns, stops in arrival order. */
    NAIVE("First come, first served: vehicles take turns, ignoring priority and distance");

    private final String description;

    StrategyType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static Optional<StrategyType> fromName(String name) {
        return Arrays.stream(values())
                .filter(t -> t.name().equalsIgnoreCase(name == null ? "" : name.trim()))
                .findFirst();
    }

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }
}
