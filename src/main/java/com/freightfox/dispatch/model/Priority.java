package com.freightfox.dispatch.model;

/**
 * Delivery priority. Lower {@code rank} is dispatched first.
 */
public enum Priority {
    HIGH(1),
    MEDIUM(2),
    LOW(3);

    private final int rank;

    Priority(int rank) {
        this.rank = rank;
    }

    public int getRank() {
        return rank;
    }
}
