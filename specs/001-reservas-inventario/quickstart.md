# Quickstart: Reservas de inventario

## Prerequisites

- Java 21
- Maven

## Validate the feature

From the repository root, run:

```bash
mvn test
```

Expected: Maven compiles against Java 21 and all existing and feature tests pass.

## Behavioral scenarios to cover

1. Register a product, add stock, reserve units, and verify available stock falls by the reserved quantity.
2. Attempt reservations concurrently whose combined quantity exceeds stock; verify total active plus sold units never exceed on-hand stock and at least one request is rejected.
3. Retry the same order while active; verify no second reservation is created. Advance a fixed clock to expiry, release it, then retry and verify a new attempt ID is created if stock is available.
4. Confirm an attempt before expiry; verify available stock remains reduced as sold units. Repeat confirmation and verify no second adjustment.
5. Expire attempt A, create attempt B with the same order ID, then approve attempt A; verify B remains active and inventory is unchanged.
6. Exercise all category payment windows and the `FLASH_SALE` two-unit maximum using a fixed clock.
7. Cross the five-unit available-stock threshold and verify one listener call; make further reductions and verify silence; restock above five and cross again to verify a second call.
8. Verify invalid quantities, unknown products, insufficient stock, and over-limit orders fail without partial stock changes.

The exact attempt-aware approval interface and the resolution of the category extensibility gate must be agreed before implementing these scenarios.

