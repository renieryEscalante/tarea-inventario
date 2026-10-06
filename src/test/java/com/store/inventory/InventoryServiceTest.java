package com.store.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.store.inventory.api.InsufficientStockException;
import com.store.inventory.api.InventoryService;
import com.store.inventory.api.OrderLimitExceededException;
import com.store.inventory.api.ProductCategory;
import com.store.inventory.api.Reservation;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InventoryServiceTest {

    private static final Instant START = Instant.parse("2026-10-06T12:00:00Z");

    private MutableClock clock;
    private InventoryService service;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(START);
        service = Inventory.create(clock, (sku, available) -> { });
        service.registerProduct("SKU-1", ProductCategory.STANDARD);
    }

    @Test
    void reservingReducesAvailableUnits() {
        service.addStock("SKU-1", 10);

        service.reserve("ORDER-1", "SKU-1", 3);

        assertEquals(7, service.available("SKU-1"));
    }

    @Test
    void cannotReserveMoreThanAvailable() {
        service.addStock("SKU-1", 2);

        assertThrows(InsufficientStockException.class, () -> service.reserve("ORDER-1", "SKU-1", 3));
        assertEquals(2, service.available("SKU-1"));
    }

    @Test
    void confirmedUnitsStaySold() {
        service.addStock("SKU-1", 5);
        service.reserve("ORDER-1", "SKU-1", 2);

        service.confirm("ORDER-1");

        assertEquals(3, service.available("SKU-1"));
    }

    @Test
    void rejectsZeroOrNegativeQuantitiesWithoutChangingStock() {
        service.addStock("SKU-1", 5);

        assertThrows(IllegalArgumentException.class, () -> service.addStock("SKU-1", 0));
        assertThrows(IllegalArgumentException.class, () -> service.addStock("SKU-1", -1));
        assertThrows(IllegalArgumentException.class, () -> service.reserve("ORDER-1", "SKU-1", 0));
        assertThrows(IllegalArgumentException.class, () -> service.reserve("ORDER-1", "SKU-1", -1));
        assertEquals(5, service.available("SKU-1"));
    }

    @Test
    void activeRetryReturnsSameReservationWithoutReservingTwice() {
        service.addStock("SKU-1", 10);

        Reservation first = service.reserve("ORDER-1", "SKU-1", 3);
        Reservation retry = service.reserve("ORDER-1", "SKU-1", 3);

        assertEquals(first, retry);
        assertEquals(7, service.available("SKU-1"));
    }

    @Test
    void concurrentReservationsNeverExceedAvailableStock() throws Exception {
        service.addStock("SKU-1", 10);
        int requestCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(requestCount);
        CountDownLatch ready = new CountDownLatch(requestCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();

        try {
            for (int i = 0; i < requestCount; i++) {
                String orderId = "ORDER-" + i;
                results.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    try {
                        service.reserve(orderId, "SKU-1", 1);
                        return true;
                    } catch (InsufficientStockException exception) {
                        return false;
                    }
                }));
            }

            ready.await();
            start.countDown();
            int successful = 0;
            for (Future<Boolean> result : results) {
                if (result.get()) {
                    successful++;
                }
            }

            assertEquals(10, successful);
            assertEquals(0, service.available("SKU-1"));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void failedCartLineDoesNotAffectIndependentProductOrders() {
        service.registerProduct("SKU-2", ProductCategory.STANDARD);
        service.addStock("SKU-1", 1);
        service.addStock("SKU-2", 2);

        assertThrows(InsufficientStockException.class, () -> service.reserve("ORDER-1", "SKU-1", 2));
        service.reserve("ORDER-2", "SKU-2", 1);

        assertEquals(1, service.available("SKU-1"));
        assertEquals(1, service.available("SKU-2"));
    }

    @Test
    void appliesDefaultCategoryDurationsAndFlashSaleLimit() {
        service.registerProduct("PRE", ProductCategory.PRE_ORDER);
        service.registerProduct("FLASH", ProductCategory.FLASH_SALE);
        service.addStock("SKU-1", 10);
        service.addStock("PRE", 10);
        service.addStock("FLASH", 10);

        Reservation standard = service.reserve("STANDARD-ORDER", "SKU-1", 1);
        Reservation preOrder = service.reserve("PRE-ORDER", "PRE", 1);
        Reservation flashSale = service.reserve("FLASH-ORDER", "FLASH", 2);

        assertEquals(START.plus(Duration.ofMinutes(15)), standard.expiresAt());
        assertEquals(START.plus(Duration.ofHours(24)), preOrder.expiresAt());
        assertEquals(START.plus(Duration.ofMinutes(5)), flashSale.expiresAt());
        assertThrows(OrderLimitExceededException.class, () -> service.reserve("FLASH-TOO-MANY", "FLASH", 3));
        assertEquals(8, service.available("FLASH"));
    }

    @Test
    void acceptsCustomCategoryPoliciesWithoutChangingDefaultInstances() {
        EnumMap<ProductCategory, CategoryPolicy> values = new EnumMap<>(ProductCategory.class);
        values.put(ProductCategory.STANDARD, new CategoryPolicy(Duration.ofMinutes(2), OptionalInt.of(4)));
        values.put(ProductCategory.PRE_ORDER, new CategoryPolicy(Duration.ofHours(3), OptionalInt.empty()));
        values.put(ProductCategory.FLASH_SALE, new CategoryPolicy(Duration.ofMinutes(4), OptionalInt.of(1)));
        InventoryPolicies policies = new InventoryPolicies(values);
        InventoryService custom = Inventory.create(clock, (sku, available) -> { }, policies);
        custom.registerProduct("CUSTOM", ProductCategory.STANDARD);
        custom.addStock("CUSTOM", 10);
        service.addStock("SKU-1", 10);

        Reservation customReservation = custom.reserve("CUSTOM-ORDER", "CUSTOM", 4);
        Reservation standardReservation = service.reserve("DEFAULT-ORDER", "SKU-1", 1);

        assertEquals(START.plus(Duration.ofMinutes(2)), customReservation.expiresAt());
        assertEquals(START.plus(Duration.ofMinutes(15)), standardReservation.expiresAt());
        assertThrows(OrderLimitExceededException.class, () -> custom.reserve("CUSTOM-LIMIT", "CUSTOM", 5));
    }

    @Test
    void rejectsInvalidCategoryPolicyValuesAndIncompletePolicyMaps() {
        assertThrows(IllegalArgumentException.class,
                () -> new CategoryPolicy(Duration.ZERO, OptionalInt.empty()));
        assertThrows(IllegalArgumentException.class,
                () -> new CategoryPolicy(Duration.ofMinutes(1), OptionalInt.of(0)));

        Map<ProductCategory, CategoryPolicy> incomplete = Map.of(
                ProductCategory.STANDARD,
                new CategoryPolicy(Duration.ofMinutes(1), OptionalInt.empty()));
        assertThrows(IllegalArgumentException.class, () -> new InventoryPolicies(incomplete));
    }

    @Test
    void releasesReservationAtInclusiveDeadlineAndAllowsNewAttemptWithSameOrderId() {
        service.addStock("SKU-1", 5);
        service.reserve("ORDER-1", "SKU-1", 3);
        clock.advance(Duration.ofMinutes(15));

        assertEquals(5, service.available("SKU-1"));
        Reservation retry = service.reserve("ORDER-1", "SKU-1", 4);

        assertEquals(1, service.available("SKU-1"));
        assertEquals(START.plus(Duration.ofMinutes(30)), retry.expiresAt());
    }

    @Test
    void repeatedExpiryDoesNotReleaseUnitsMoreThanOnce() {
        service.addStock("SKU-1", 5);
        service.reserve("ORDER-1", "SKU-1", 3);
        clock.advance(Duration.ofMinutes(15));

        assertEquals(5, service.available("SKU-1"));
        assertEquals(5, service.available("SKU-1"));
    }

    @Test
    void latePaymentConfirmsNewestActiveAttemptForOrder() {
        service.addStock("SKU-1", 10);
        service.reserve("ORDER-1", "SKU-1", 2);
        clock.advance(Duration.ofMinutes(15));
        service.reserve("ORDER-1", "SKU-1", 3);

        service.confirm("ORDER-1");

        assertEquals(7, service.available("SKU-1"));
    }

    @Test
    void confirmationIsIdempotentAndRequiresAnActiveReservation() {
        service.addStock("SKU-1", 5);
        service.reserve("ORDER-1", "SKU-1", 2);

        service.confirm("ORDER-1");
        assertThrows(IllegalStateException.class, () -> service.confirm("ORDER-1"));

        assertEquals(3, service.available("SKU-1"));
        assertThrows(IllegalStateException.class, () -> service.confirm("UNKNOWN-ORDER"));
    }

    @Test
    void availableReturnsZeroForUnknownProducts() {
        assertEquals(0, service.available("UNKNOWN-SKU"));
    }

    @Test
    void emitsOneAlertWhenAvailabilityCrossesThresholdAndRearmsAfterRestock() {
        List<String> alerts = new ArrayList<>();
        InventoryService alerting = Inventory.create(clock,
                (sku, available) -> alerts.add(sku + ":" + available));
        alerting.registerProduct("SKU-ALERT", ProductCategory.STANDARD);
        alerting.addStock("SKU-ALERT", 6);

        alerting.reserve("ORDER-1", "SKU-ALERT", 1);
        alerting.reserve("ORDER-2", "SKU-ALERT", 1);
        assertEquals(List.of("SKU-ALERT:5"), alerts);

        alerting.addStock("SKU-ALERT", 2);
        alerting.reserve("ORDER-3", "SKU-ALERT", 2);

        assertEquals(List.of("SKU-ALERT:5", "SKU-ALERT:4"), alerts);
    }

    @Test
    void listenerFailureDoesNotUndoStockChangeOrRetryUntilRestockAboveThreshold() {
        AtomicInteger attempts = new AtomicInteger();
        InventoryService alerting = Inventory.create(clock, (sku, available) -> {
            attempts.incrementAndGet();
            throw new IllegalStateException("notification failed");
        });
        alerting.registerProduct("SKU-ALERT", ProductCategory.STANDARD);
        alerting.addStock("SKU-ALERT", 6);

        alerting.reserve("ORDER-1", "SKU-ALERT", 1);
        alerting.reserve("ORDER-2", "SKU-ALERT", 1);
        assertEquals(4, alerting.available("SKU-ALERT"));
        assertEquals(1, attempts.get());

        alerting.addStock("SKU-ALERT", 1);
        assertEquals(1, attempts.get());
        alerting.addStock("SKU-ALERT", 1);
        alerting.reserve("ORDER-3", "SKU-ALERT", 1);

        assertEquals(2, attempts.get());
        assertEquals(5, alerting.available("SKU-ALERT"));
    }

    @Test
    void listenerCannotPreventSuccessfulReservationFromBeingReturned() {
        InventoryService alerting = Inventory.create(clock, (sku, available) -> {
            throw new IllegalArgumentException("notification failed");
        });
        alerting.registerProduct("SKU-ALERT", ProductCategory.STANDARD);
        alerting.addStock("SKU-ALERT", 6);

        Reservation reservation = alerting.reserve("ORDER-1", "SKU-ALERT", 1);

        assertEquals("ORDER-1", reservation.orderId());
        assertEquals(5, alerting.available("SKU-ALERT"));
    }

    @Test
    void reservationPolicyCopiesInputMapAndExposesImmutablePolicies() {
        EnumMap<ProductCategory, CategoryPolicy> values = defaultsMap();
        InventoryPolicies policies = new InventoryPolicies(values);
        values.clear();

        assertEquals(3, policies.asMap().size());
        assertThrows(UnsupportedOperationException.class, () -> policies.asMap().clear());
    }

    private static EnumMap<ProductCategory, CategoryPolicy> defaultsMap() {
        EnumMap<ProductCategory, CategoryPolicy> values = new EnumMap<>(ProductCategory.class);
        values.put(ProductCategory.STANDARD,
                new CategoryPolicy(Duration.ofMinutes(15), OptionalInt.empty()));
        values.put(ProductCategory.PRE_ORDER,
                new CategoryPolicy(Duration.ofHours(24), OptionalInt.empty()));
        values.put(ProductCategory.FLASH_SALE,
                new CategoryPolicy(Duration.ofMinutes(5), OptionalInt.of(2)));
        return values;
    }

    private static final class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }
    }
}
