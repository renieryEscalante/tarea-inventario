# Data Model: Reservas de inventario

## Product

Represents one registered SKU and its inventory state.

| Field | Type | Rules |
|---|---|---|
| `sku` | String | Stable key; registered before stock can be added. |
| `category` | `ProductCategory` | Required; selects a reservation policy. |
| `onHand` | non-negative integer | Increased only by valid restock operations. |
| `sold` | non-negative integer | Increased when an active reservation is confirmed. |
| `activeReservationUnits` | non-negative integer | Sum of units in active, unexpired reservation attempts. |
| `lowStockAlertLatched` | boolean | Set after alerting at available stock <= 5; cleared only by a restock that leaves available stock > 5. |

Available units are `onHand - sold - activeReservationUnits`; this value must never be negative. Unknown SKUs report zero available.

## Order identity

An order ID is supplied by the caller and is stable across retries. At most one non-expired reservation attempt is active for a given order ID. A rejected request creates no attempt. If the previous attempt has expired and been released, the same order ID may create a new attempt if stock allows.

## Reservation attempt

| Field | Type | Rules |
|---|---|---|
| `attemptId` | unique opaque identifier | New value for each successful reservation generation; payment approval must name it. |
| `orderId` | String | Caller-supplied stable identity. |
| `sku` | String | Registered product. |
| `quantity` | positive integer | Must satisfy category policy and available stock. |
| `createdAt` | Instant | Obtained from injected `Clock`. |
| `expiresAt` | Instant | `createdAt + category payment window`. |
| `status` | enum | `ACTIVE`, `RELEASED`, or `CONFIRMED`; terminal states cannot transition again. |

Transitions:

```text
ACTIVE --confirm matching attempt before expiry--> CONFIRMED
ACTIVE --time reaches expiresAt / expiration sweep--> RELEASED
RELEASED and CONFIRMED are terminal
```

Confirming or releasing an attempt is idempotent. A stale approval for a released attempt does not affect a later attempt with the same order ID.

## Category policy

| Category | Payment window | Maximum units per order |
|---|---:|---:|
| `STANDARD` | 15 minutes | Unbounded by category |
| `PRE_ORDER` | 24 hours | Unbounded by category |
| `FLASH_SALE` | 5 minutes | 2 |

The policy should be data-driven internally. Adding a category remains blocked by the current immutable public enum and registration signature (see plan gate).

## Low-stock alert state

An alert is associated with a SKU and reports its available unit count at the threshold transition. When availability first reaches five or less while unlatched, notify `StockAlertListener` and latch. Suppress further alerts while latched. A valid `addStock` operation that leaves availability above five clears the latch, allowing a later crossing to notify again.

