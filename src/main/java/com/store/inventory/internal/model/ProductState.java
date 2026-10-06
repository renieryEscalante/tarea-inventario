package com.store.inventory.internal.model;

import com.store.inventory.api.ProductCategory;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Mutable in-memory state for one registered product. */
public final class ProductState {

    private final String sku;
    private final ProductCategory category;
    private final List<ReservationAttempt> reservations = new ArrayList<>();
    private int receivedUnits;
    private int soldUnits;
    private boolean lowStockAlertArmed = true;

    public ProductState(String sku, ProductCategory category) {
        this.sku = sku;
        this.category = category;
    }

    public String sku() {
        return sku;
    }

    public ProductCategory category() {
        return category;
    }

    public void addStock(int quantity) {
        receivedUnits = Math.addExact(receivedUnits, quantity);
    }

    public int availableUnits() {
        int reservedUnits = reservations.stream()
                .filter(ReservationAttempt::isActive)
                .mapToInt(attempt -> attempt.reservation().quantity())
                .sum();
        return receivedUnits - soldUnits - reservedUnits;
    }

    public void addReservation(ReservationAttempt attempt) {
        reservations.add(attempt);
    }

    public void expireDueReservations(Instant now) {
        reservations.forEach(attempt -> attempt.expireIfDue(now));
    }

    public void recordSale(int quantity) {
        soldUnits = Math.addExact(soldUnits, quantity);
    }

    public boolean isLowStockAlertArmed() {
        return lowStockAlertArmed;
    }

    public void markLowStockAlertAttempted() {
        lowStockAlertArmed = false;
    }

    public void rearmLowStockAlert() {
        lowStockAlertArmed = true;
    }
}
