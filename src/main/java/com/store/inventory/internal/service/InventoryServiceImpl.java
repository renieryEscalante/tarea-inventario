package com.store.inventory.internal.service;

import com.store.inventory.CategoryPolicy;
import com.store.inventory.InventoryPolicies;
import com.store.inventory.api.InsufficientStockException;
import com.store.inventory.api.InventoryService;
import com.store.inventory.api.OrderLimitExceededException;
import com.store.inventory.api.ProductCategory;
import com.store.inventory.api.Reservation;
import com.store.inventory.api.StockAlertListener;
import com.store.inventory.internal.model.ProductState;
import com.store.inventory.internal.model.ReservationAttempt;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

/** In-memory implementation of the inventory contract. */
public final class InventoryServiceImpl implements InventoryService {

    private static final int LOW_STOCK_THRESHOLD = 5;

    private final Object lock = new Object();
    private final Clock clock;
    private final StockAlertListener alertListener;
    private final InventoryPolicies policies;
    private final Map<String, ProductInventory> products = new HashMap<>();
    private final Map<String, List<ReservationAttempt>> attemptsByOrder = new HashMap<>();

    public InventoryServiceImpl(Clock clock, StockAlertListener alertListener, InventoryPolicies policies) {
        if (clock == null || alertListener == null || policies == null) {
            throw new IllegalArgumentException("Clock, alert listener, and policies are required");
        }
        this.clock = clock;
        this.alertListener = alertListener;
        this.policies = policies;
    }

    @Override
    public void registerProduct(String sku, ProductCategory category) {
        requireText(sku, "sku");
        if (category == null) {
            throw new IllegalArgumentException("category is required");
        }

        synchronized (lock) {
            if (products.containsKey(sku)) {
                throw new IllegalArgumentException("Product is already registered: " + sku);
            }
            products.put(sku, new ProductInventory(sku, category));
        }
    }

    @Override
    public void addStock(String sku, int quantity) {
        requireText(sku, "sku");
        requirePositive(quantity, "quantity");

        synchronized (lock) {
            expireAllReservations(clock.instant());
            ProductInventory product = products.get(sku);
            if (product == null) {
                throw new IllegalArgumentException("Product is not registered: " + sku);
            }

            ProductState state = product.state();
            state.addStock(quantity);
            if (state.availableUnits() > LOW_STOCK_THRESHOLD) {
                state.rearmLowStockAlert();
            }
        }
    }

    @Override
    public Reservation reserve(String orderId, String sku, int quantity) {
        requireText(orderId, "orderId");
        requireText(sku, "sku");
        requirePositive(quantity, "quantity");

        Reservation result;
        LowStockAlert alert = null;
        synchronized (lock) {
            Instant now = clock.instant();
            expireAllReservations(now);

            List<ReservationAttempt> previousAttempts = attemptsByOrder.get(orderId);
            if (previousAttempts != null) {
                for (int i = previousAttempts.size() - 1; i >= 0; i--) {
                    ReservationAttempt attempt = previousAttempts.get(i);
                    if (attempt.isActive()) {
                        return attempt.reservation();
                    }
                }
            }

            ProductInventory product = products.get(sku);
            if (product == null) {
                throw new InsufficientStockException(sku, quantity, 0);
            }

            ProductState state = product.state();
            CategoryPolicy policy = policies.policyFor(state.category());
            OptionalInt limit = policy.maxUnitsPerOrder();
            if (limit.isPresent() && quantity > limit.getAsInt()) {
                throw new OrderLimitExceededException(sku, quantity, limit.getAsInt());
            }

            int availableBefore = product.availableUnits();
            if (quantity > availableBefore) {
                throw new InsufficientStockException(sku, quantity, availableBefore);
            }

            Reservation reservation = new Reservation(
                    orderId,
                    sku,
                    quantity,
                    now.plus(policy.reservationDuration()));
            ReservationAttempt attempt = new ReservationAttempt(reservation);
            state.addReservation(attempt);
            attemptsByOrder.computeIfAbsent(orderId, ignored -> new ArrayList<>()).add(attempt);
            result = reservation;

            int availableAfter = product.availableUnits();
            if (state.isLowStockAlertArmed()
                    && availableBefore > LOW_STOCK_THRESHOLD
                    && availableAfter <= LOW_STOCK_THRESHOLD) {
                state.markLowStockAlertAttempted();
                alert = new LowStockAlert(sku, availableAfter);
            }
        }

        if (alert != null) {
            notifyListener(alert);
        }
        return result;
    }

    @Override
    public void confirm(String orderId) {
        requireText(orderId, "orderId");

        synchronized (lock) {
            expireAllReservations(clock.instant());
            List<ReservationAttempt> attempts = attemptsByOrder.get(orderId);
            if (attempts != null) {
                for (int i = attempts.size() - 1; i >= 0; i--) {
                    ReservationAttempt attempt = attempts.get(i);
                    if (attempt.isActive()) {
                        attempt.confirm();
                        ProductInventory product = products.get(attempt.reservation().sku());
                        product.state().recordSale(attempt.reservation().quantity());
                        return;
                    }
                }
            }
            throw new IllegalStateException("Order has no active reservation: " + orderId);
        }
    }

    @Override
    public int available(String sku) {
        synchronized (lock) {
            expireAllReservations(clock.instant());
            ProductInventory product = products.get(sku);
            return product == null ? 0 : product.availableUnits();
        }
    }

    private void expireAllReservations(Instant now) {
        products.values().forEach(product -> product.state().expireDueReservations(now));
    }

    private void notifyListener(LowStockAlert alert) {
        try {
            alertListener.onLowStock(alert.sku(), alert.availableUnits());
        } catch (RuntimeException ignored) {
            // Inventory changes are committed even if the alert receiver fails.
        }
    }

    private static void requirePositive(int quantity, String name) {
        if (quantity <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }

    private record LowStockAlert(String sku, int availableUnits) {
    }
}
