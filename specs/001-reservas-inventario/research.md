# Research: Reservas de inventario

## Repository findings

### Java, build, and tests

- **Decision**: Plan for Java 21 and Maven; retain JUnit Jupiter/Surefire for validation.
- **Rationale**: `pom.xml` sets `maven.compiler.release` to 21 and already declares JUnit Jupiter 5.10.2 and Maven Surefire 3.2.5.
- **Alternatives considered**: Introducing another language or build system is unsupported by the existing repository and not required by the feature.

### Service entry point and public boundary

- **Decision**: Construct the implementation in `Inventory.create(Clock, StockAlertListener)` and keep implementation types outside `com.store.inventory.api`.
- **Rationale**: The factory is the explicit construction seam and the constitution protects the API package and factory signature.
- **Alternatives considered**: Modifying `InventoryService`, `Reservation`, `ProductCategory`, or the listener would break the stated compatibility constraint.

### State and concurrency

- **Decision**: Use a single in-memory repository with atomic per-operation state transitions. Store on-hand stock and sold quantity, and derive available stock after excluding active reservations.
- **Rationale**: The repository is explicitly in-memory for this iteration; concurrent reserve calls must not assign more units than remain available. A single synchronization boundary is straightforward for this single-process scope.
- **Alternatives considered**: Unsynchronized maps permit overselling; a database or distributed lock is out of scope and belongs to the later multi-instance migration.

### Time and expiration

- **Decision**: Inject the supplied `Clock`; store absolute expiry instants and consider an attempt expired when `clock.instant()` is equal to or after its expiry.
- **Rationale**: Deterministic time is already part of the factory contract and the spec says the exact expiry instant is expired.
- **Alternatives considered**: System clock calls inside the service defeat deterministic behavior; background timers are unnecessary because operations can lazily expire due attempts and a sweep operation can release all due attempts.

### Retry identity and late payment

- **Decision**: Give every reservation generation a unique attempt ID and retain attempt history by that ID. Add an attempt-aware payment approval interface in an implementation-owned package.
- **Rationale**: The spec permits reusing an order ID after expiry and explicitly requires an old payment approval not to affect a later attempt. `InventoryService.confirm(String orderId)` lacks enough information to guarantee that rule.
- **Alternatives considered**: Reusing only order ID is ambiguous; changing the protected public API is disallowed. Keep the legacy method for compatibility, while payment integrations use the extension.

### Category policies and specification limits

- **Decision**: Represent current category durations and limits as policy data, not scattered branches.
- **Rationale**: `STANDARD`, `PRE_ORDER`, and `FLASH_SALE` have distinct rules and should be easy to audit.
- **Alternatives considered**: A switch statement is simpler initially but less maintainable.
- **Open constraint**: `ProductCategory` is a fixed enum in the protected API and `registerProduct` accepts only that enum. This means a new category cannot be introduced without changing the protected API or defining a separate registration contract. FR-008 remains unsatisfied until the spec/constitution clarifies the allowed extension mechanism.

### Spring Boot and controller architecture

- **Decision**: Do not assume HTTP endpoints; preserve the endpoint-free scope. The Spring Boot/controller mandate needs a product-level decision before implementation.
- **Rationale**: The constitution requires Spring Boot and Controller/Service/Repository/Model separation, while FR-015 and the README explicitly exclude endpoints. The current `pom.xml` is a plain Java library with no Spring dependency.
- **Alternatives considered**: Adding a web controller would violate scope; adding Spring Boot solely for the factory has no established benefit and could affect packaging/dependencies.
- **Open constraint**: Decide whether Spring Boot is required only for dependency wiring (with a non-HTTP adapter) or whether the constitution should be amended for this in-process service.

### Alerts

- **Decision**: Keep alert delivery behind the existing `StockAlertListener`; maintain per-product latch state and rearm only when `addStock` raises availability above five.
- **Rationale**: This matches the protected listener contract and constitution. Alert decisions occur with the stock state transition to avoid duplicate threshold-crossing notifications.
- **Alternatives considered**: Direct mail/channel delivery couples domain logic to infrastructure and violates the listener boundary.

