package com.store.inventory.internal.model;

import com.store.inventory.api.Reservation;
import java.time.Instant;

/** One internal generation of a reservation for an order. */
public final class ReservationAttempt {

    public enum State {
        ACTIVE,
        EXPIRED,
        CONFIRMED
    }

    private final Reservation reservation;
    private State state = State.ACTIVE;

    public ReservationAttempt(Reservation reservation) {
        this.reservation = reservation;
    }

    public Reservation reservation() {
        return reservation;
    }

    public State state() {
        return state;
    }

    public boolean isActive() {
        return state == State.ACTIVE;
    }

    public boolean expireIfDue(Instant now) {
        if (isActive() && !now.isBefore(reservation.expiresAt())) {
            state = State.EXPIRED;
            return true;
        }
        return false;
    }

    public boolean confirm() {
        if (!isActive()) {
            return false;
        }
        state = State.CONFIRMED;
        return true;
    }
}
