# Inventory Service Contract

## Existing public contract

The public Java contract remains `com.store.inventory.api.InventoryService`, created through `Inventory.create(Clock, StockAlertListener)`. The API package and factory signature are immutable for this feature.

| Operation | Contract |
|---|---|
| `registerProduct(sku, category)` | Register a product before stock operations. |
| `addStock(sku, quantity)` | Add positive stock; reject non-positive quantity or unregistered SKU. This is the only restock/rearm operation. |
| `reserve(orderId, sku, quantity)` | Atomically create or return the active reservation for an idempotent retry; reject invalid quantity, category limit, or insufficient stock without partial allocation. |
| `confirm(orderId)` | Compatibility operation confirming the active reservation for the order. Does not carry enough identity to distinguish a stale payment callback after order ID reuse. |
| `available(sku)` | Return on-hand less sold and active reservations; zero for unknown SKU. |

Reservations expire at `now >= expiresAt`. Reservation and release/confirmation transitions must not double-adjust inventory.

## Attempt-aware payment approval extension

Implementation-owned contract: `com.store.inventory.internal.payment.PaymentApprovalService` (name/package to be finalized during implementation).

```text
confirm(orderId, attemptId) -> outcome
```

The caller must provide both identifiers from the reservation response. The operation confirms only the matching active attempt before expiration. A missing, expired, released, or mismatched attempt is not allowed to confirm a later attempt with the same order ID. Repeated approval of an already confirmed attempt is idempotent.

This extension is necessary to meet the clarified late-payment requirement without changing `InventoryService`. The payment integration itself remains outside scope.

## Alert listener

`StockAlertListener.onLowStock(sku, availableUnits)` is invoked once when a product first reaches <= 5 available units. It is rearmed only by a restock that raises available units above 5. Delivery channel remains the listener implementation's responsibility.

## Boundaries

- No HTTP endpoints, authentication, or payment gateway.
- Category values are constrained by the existing public enum. FR-008 needs a compatible public extension or spec change before it can be satisfied.

