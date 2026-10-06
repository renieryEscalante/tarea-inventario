# Implementation Plan: Reservas de inventario

**Branch**: `001-reservas-inventario` | **Date**: 2026-10-06 | **Spec**: [spec.md](spec.md)

## Summary

Implementar el ciclo de reservas en memoria detrás de `Inventory.create(Clock, StockAlertListener)`, preservando los archivos de `com.store.inventory.api`. El diseño previsto mantiene estado de productos y reservas bajo coordinación segura ante concurrencia, aplica políticas de categoría configurables y emite alertas por `StockAlertListener`. El diseño queda bloqueado para implementación hasta resolver la incompatibilidad de categorías nuevas y el modo público de identificar intentos de pago.

## Technical Context

**Language/Version**: Java 21
**Primary Dependencies**: Maven; JUnit Jupiter 5.10.2 para pruebas. No se requiere Spring Boot.
**Storage**: Estado en memoria durante la vida de una instancia del servicio.
**Testing**: Maven Surefire y JUnit Jupiter; `InventoryServiceTest` es la cobertura existente.
**Target Platform**: JVM con Java 21.
**Project Type**: Biblioteca/servicio Java en proceso, sin endpoints ni autenticación.
**Performance Goals**: Mantener invariantes ante solicitudes concurrentes; no se especifica objetivo de rendimiento.
**Constraints**: No modificar `com.store.inventory.api` ni la firma `Inventory.create(Clock, StockAlertListener)`. Vencimiento inclusivo (`now >= expiresAt`). Alertas calculadas sobre disponibilidad, con rearme solo tras reabastecimiento por encima de cinco.
**Scale/Scope**: Una JVM e instancia en memoria; persistencia y coordinación multi-instancia están fuera del alcance.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate | Result | Notes |
|---|---|---|
| Conservar contrato público y fábrica | PASS | El diseño mantiene intactos los archivos API existentes y la firma de fábrica. |
| Java 21, Maven y memoria; sin endpoints | PASS | Coincide con constitución, `pom.xml` y alcance de `spec.md`. Spring Boot/controladores no son requisitos. |
| Ciclo de reserva, políticas conocidas y alertas desacopladas | PASS | Las reglas STANDARD, PRE_ORDER y FLASH_SALE y `StockAlertListener` se pueden implementar con el contrato actual. |
| Admitir nuevas categorías sin cambiar API protegida | ERROR | `ProductCategory` es un enum cerrado y `registerProduct` solo acepta ese enum. No se puede registrar una categoría nueva en tiempo de ejecución sin cambiar el API protegido o reinterpretar FR-008. |
| Aprobación ligada al intento de reserva | ERROR | La interfaz existente solo expone `confirm(orderId)` y `Reservation` no contiene intento. Un contrato adicional es técnicamente posible, pero `Inventory.create` devuelve solo `InventoryService` y el spec no define cómo el consumidor obtiene el ID y entrega la aprobación. |
| Sin decisiones de dominio pendientes | ERROR | FR-008 y FR-009 requieren aclaración/ajuste antes de implementar. No se descartan silenciosamente. |

La investigación y el diseño documental continúan para hacer concretos los puntos de decisión. El gate ERROR debe resolverse en los artefactos rectores antes de comenzar cambios de código, conforme a la constitución.

## Project Structure

### Documentación de esta funcionalidad

```text
specs/001-reservas-inventario/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
└── contracts/
    └── inventory-service.md
```

### Código fuente

```text
src/main/java/com/store/inventory/
├── Inventory.java
├── api/                         # contrato existente, protegido
└── internal/
    ├── service/                 # operaciones de inventario y políticas
    ├── model/                   # estado del producto, intento de reserva
    └── payment/                 # contrato adicional de aprobación, pendiente de decisión

src/test/java/com/store/inventory/
└── InventoryServiceTest.java
```

**Structure Decision**: Mantener el único módulo Maven y las clases internas bajo `com.store.inventory.internal`. Separar el estado coordinado del flujo de servicio y de las políticas solo donde el dominio lo requiera. No crear capa HTTP.

## Phase 0: Research

Hallazgos y decisiones están en [research.md](research.md). Las decisiones técnicas verificables quedaron resueltas mediante inspección del repositorio. Las incompatibilidades FR-008 y FR-009 se registran como decisiones de producto pendientes, no como supuestos de implementación.

## Phase 1: Design & Contracts

- [Modelo de datos](data-model.md): producto, inventario, intento, política y estado de alerta.
- [Contrato de servicio](contracts/inventory-service.md): interfaz actual y extensión necesaria aún por acordar.
- [Guía de validación](quickstart.md): escenarios de aceptación ejecutables una vez resueltas las decisiones de contrato.

## Constitution Check (post-design)

| Gate | Result | Notes |
|---|---|---|
| Arquitectura y dependencias | PASS | Diseño en memoria, Java 21/Maven, sin framework web. |
| API protegido | PASS | No se propone editar tipos existentes en `com.store.inventory.api`. |
| FR-008 categorías extensibles | ERROR | Enum cerrado no permite categorías nuevas con la firma actual. |
| FR-009 aprobación de intento específico | ERROR | Hace falta definir cómo se expone el intento y cómo el procesador de pago lo devuelve. |

## Complexity Tracking

| Issue | Why needed | Simpler alternative rejected because |
|---|---|---|
| Identidad de intento además de `orderId` | Evita que una aprobación tardía confirme una reserva posterior del mismo pedido. | `confirm(orderId)` no diferencia generaciones. La forma pública de exponerla está pendiente. |
| Registro de políticas por categoría | Mantiene plazos y límites fuera del mecanismo de reserva. | Condicionales rígidos no cubren políticas estacionales; aun así, el enum protegido impide incorporar nuevas claves de categoría. |
