# Implementation Plan: Reservas de inventario

**Branch**: `001-reservas-inventario` | **Date**: 2026-10-06 | **Spec**: [spec.md](spec.md)

## Summary

Implement the in-memory `InventoryService` behind the existing `Inventory.create(Clock, StockAlertListener)` factory. Use a thread-safe repository for products and reservation attempts, category policies for expiration and limits, and a listener port for low-stock alerts. Preserve the protected `com.store.inventory.api` files and do not add HTTP endpoints or authentication. A separate payment-approval interface can carry the reservation-attempt ID needed to reject stale approvals while retaining the existing `InventoryService.confirm(orderId)` contract.

## Technical Context

**Language/Version**: Java 21 (Maven compiler release is 21)
**Primary Dependencies**: Spring Boot is required by the constitution but is not present in the current `pom.xml`; its exact role must be resolved before implementation. JUnit Jupiter 5.10.2 is already configured for tests.
**Storage**: In-memory repository; product stock, sold units, reservations, and alert latch live for the service instance lifetime.
**Testing**: Maven Surefire and JUnit Jupiter; existing `InventoryServiceTest` is the baseline.
**Target Platform**: Java 21 runtime
**Project Type**: In-process Java service/library with a stable public Java contract; no HTTP interface.
**Performance Goals**: Preserve inventory invariants under concurrent requests; no throughput target is specified.
**Constraints**: Do not modify `com.store.inventory.api` or the factory signature. No authentication, endpoints, or external payment gateway. Reservations expire at `now >= expiresAt`. Stock alerts are based on available (on-hand minus reserved) stock, with rearming only after a stock addition leaves availability above five.
**Scale/Scope**: Single JVM and in-memory state for this iteration; persistence and multi-instance coordination are out of scope.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate | Result | Notes |
|---|---|---|
| Preserve public contract and factory signature | PASS | All implementation is behind the existing factory and APIs. |
| Spring Boot, Java 21, Maven, in-memory storage | NEEDS DECISION | Java 21/Maven/in-memory match the repository; Spring Boot is constitution-required but absent from the build and has no clear role for this endpoint-free service. |
| Controller / Service / Repository / Model separation | NEEDS DECISION | Service, Repository and Model fit; a Controller conflicts with the explicit no-endpoints scope unless “Controller” means a non-HTTP adapter. |
| Reservation lifecycle, category rules, alerts | PASS WITH API LIMIT | Lifecycle and existing categories can be modeled. The protected `ProductCategory` enum prevents adding runtime categories through the current `registerProduct` signature. |
| Attempt-specific payment approval | PASS WITH EXTENSION | Add a separate approval interface carrying `orderId` and attempt ID. `InventoryService.confirm(orderId)` remains for compatibility, but cannot itself distinguish a stale callback. |
| No unresolved domain decisions | ERROR | FR-008 cannot be fulfilled as written while the API enum and registration signature are immutable. The Spring Boot/controller requirement also conflicts with the stated endpoint-free service scope. Resolve these before implementation. |

Planning can describe the implementable design, but the ERROR gate must be resolved by updating the spec or constitution before code implementation. No feature requirements are silently dropped.

## Project Structure

### Documentation (this feature)

```text
specs/001-reservas-inventario/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
└── contracts/
    └── inventory-service.md
```

### Source Code (repository root)

```text
src/main/java/com/store/inventory/
├── Inventory.java                         # existing factory; wire implementation
├── api/                                    # protected public API; unchanged
└── internal/
    ├── service/                            # reservation orchestration and category policy
    ├── repository/                         # in-memory, synchronized inventory state
    ├── model/                              # product state and reservation attempts
    └── payment/                            # attempt-aware approval extension

src/test/java/com/store/inventory/
└── InventoryServiceTest.java               # existing tests plus behavior coverage
```

**Structure Decision**: Extend the existing single Maven module under `com.store.inventory`; keep implementation classes outside the protected `api` package. The exact Spring Boot/controller arrangement remains gated as described above.

## Phase 0: Research

See [research.md](research.md). Repository inspection resolved the Java, Maven, test, entry point, API, and storage facts. The only open items are specification/constitution compatibility decisions, not technology unknowns that can safely be guessed.

## Phase 1: Design & Contracts

- [Data model](data-model.md) defines stock, reservation attempts, policy, and alert state.
- [Service contract](contracts/inventory-service.md) records existing operations and the attempt-aware extension.
- [Quickstart](quickstart.md) records local validation scenarios and expected behavior.

## Constitution Check (post-design)

The design keeps the protected API untouched and provides an attempt-aware approval path alongside it. It still cannot make the fixed enum accept new categories. Spring Boot/controller use also remains ambiguous against the explicit no-endpoints requirement. The post-design gate therefore remains **ERROR** until those conflicts are resolved in the governing artifacts.

## Complexity Tracking

| Issue | Why needed | Simpler alternative rejected because |
|---|---|---|
| Attempt-aware payment interface | Payment approval must name the precise reservation generation when an order ID is reused. | `confirm(orderId)` alone can accidentally confirm a newer reservation after an old attempt expires. |
| Separate category policy registry | Keeps durations and quantity limits out of reservation mechanics. | Hard-coding category conditionals makes seasonal policy changes harder, but the immutable enum still blocks introducing a new category through the public API. |

