package com.store.inventory;

import java.time.Duration;
import java.util.Objects;
import java.util.OptionalInt;

/** Immutable reservation rules for one product category. */
public record CategoryPolicy(Duration reservationDuration, OptionalInt maxUnitsPerOrder) {

    public CategoryPolicy {
        Objects.requireNonNull(reservationDuration, "reservationDuration");
        Objects.requireNonNull(maxUnitsPerOrder, "maxUnitsPerOrder");
        if (reservationDuration.isZero() || reservationDuration.isNegative()) {
            throw new IllegalArgumentException("reservationDuration must be positive");
        }
        if (maxUnitsPerOrder.isPresent() && maxUnitsPerOrder.getAsInt() <= 0) {
            throw new IllegalArgumentException("maxUnitsPerOrder must be positive when present");
        }
    }
}
