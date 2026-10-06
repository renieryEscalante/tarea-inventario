# Plan de implementación: Reservas de inventario

**Rama**: `001-reservas-inventario` | **Fecha**: 2026-10-06 | **Especificación**: [spec.md](spec.md)

## Resumen

Implementar reservas, vencimientos, confirmaciones y alertas en memoria detrás de `Inventory.create(Clock, StockAlertListener)`, sin modificar los archivos protegidos de `com.store.inventory.api`. Las reglas para `STANDARD`, `PRE_ORDER` y `FLASH_SALE` se representarán como políticas configurables. Una sobrecarga adicional de fábrica aceptará dichas políticas; la fábrica existente conservará sus valores predeterminados y firma. Las aprobaciones se resuelven por `orderId` y confirman la reserva activa más reciente. Una excepción de `StockAlertListener` no revierte el cambio de inventario ni rearma la alerta.

## Contexto técnico

**Lenguaje y versión**: Java 21.
**Dependencias principales**: Maven y JUnit Jupiter 5.10.2 para pruebas. No se requiere Spring Boot.
**Almacenamiento**: Estado en memoria durante la vida de la instancia.
**Pruebas**: Maven Surefire y JUnit Jupiter; `InventoryServiceTest` es la cobertura existente.
**Plataforma objetivo**: JVM con Java 21.
**Tipo de proyecto**: Biblioteca/servicio Java en proceso, sin endpoints ni autenticación.
**Objetivos de rendimiento**: Mantener las invariantes de inventario ante solicitudes concurrentes; no se especifica una meta de rendimiento.
**Restricciones**: No modificar `com.store.inventory.api` ni cambiar la firma `Inventory.create(Clock, StockAlertListener)`. El vencimiento es inclusivo (`now >= expiresAt`). Las alertas usan disponibilidad, descontando las reservas y ventas; solo se rearman cuando `addStock` supera cinco unidades disponibles.
**Escala y alcance**: Una JVM y una instancia en memoria; persistencia y coordinación entre instancias quedan fuera del alcance.

## Revisión de la constitución

| Criterio | Resultado | Notas |
|---|---|---|
| Conservar API protegido y firma de fábrica actual | PASS | No se cambia ningún archivo de `com.store.inventory.api`; se conserva `Inventory.create(Clock, StockAlertListener)`. |
| Java 21, Maven, memoria y ausencia de HTTP | PASS | Coincide con la constitución, `pom.xml` y `spec.md`. |
| Categorías dentro del alcance | PASS | Solo existen las tres categorías protegidas. Sus políticas se configuran mediante un tipo público adicional fuera del paquete protegido. |
| Ciclo de reserva y confirmación por `orderId` | PASS | Se conserva un solo intento activo por pedido; la aprobación confirma el intento activo más reciente, incluso si el aviso llegó tarde. |
| Alertas desacopladas y fallo del listener | PASS | Se usa `StockAlertListener`; una excepción de ejecución del listener no revierte el inventario ni provoca reintento antes del reabastecimiento. |
| Sin decisiones funcionales pendientes | PASS | Las decisiones antes pendientes de categorías, aprobación y errores de alertas están registradas en `spec.md`. |

## Estructura del proyecto

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
├── Inventory.java                         # fábrica existente y sobrecarga de configuración
├── InventoryPolicies.java                 # políticas configurables para categorías existentes
├── api/                                   # contrato protegido, sin cambios
└── internal/
    ├── service/                           # operaciones y ciclo de vida de reservas
    ├── model/                             # producto, stock e intentos internos
    └── policy/                            # valores predeterminados y validación de políticas

src/test/java/com/store/inventory/
└── InventoryServiceTest.java
```

**Decisión estructural**: Mantener el único módulo Maven. Añadir solo el tipo de configuración de políticas necesario para FR-008 fuera de `api`; conservar la fábrica protegida por contrato. Las mutaciones de stock y reservas se coordinarán atómicamente por producto. La notificación se hará después de actualizar el estado y su excepción de ejecución se aislará.

## Fase 0: investigación

Las decisiones y alternativas están en [research.md](research.md). El repositorio confirma Java 21, Maven, el contrato protegido, la fábrica y el listener. No hay dependencias tecnológicas desconocidas.

## Fase 1: diseño y contratos

- [Modelo de datos](data-model.md): producto, pedido, intento interno, política y estado de alerta.
- [Contrato del servicio](contracts/inventory-service.md): contrato protegido y extensión aditiva para configurar políticas.
- [Guía de validación](quickstart.md): escenarios de reserva, vencimiento, aprobación tardía, configuración y alertas.

## Revisión de la constitución después del diseño

| Criterio | Resultado | Notas |
|---|---|---|
| API protegido y fábrica original intactos | PASS | La configuración se agrega mediante una sobrecarga fuera del paquete protegido. |
| Reglas de categoría del alcance acordado | PASS | Solo se configuran `STANDARD`, `PRE_ORDER` y `FLASH_SALE`; sus valores predeterminados cumplen la especificación. |
| Aprobación tardía | PASS | La resolución por `orderId` selecciona la reserva activa más reciente, como ordena FR-009. |
| Error de alerta | PASS | Se conserva el cambio y se considera intentado el aviso; no se rearma hasta superar el umbral mediante reabastecimiento. |

## Registro de complejidad

| Elemento | Por qué se necesita | Alternativa más sencilla descartada |
|---|---|---|
| Tipo público de políticas y sobrecarga de fábrica | Permiten configurar duraciones y límites de las tres categorías sin modificar el paquete protegido. | Valores internos fijos no cumplirían la configuración acordada en FR-008. |
| Estado interno de intentos sucesivos | Distingue vencimientos, evita liberar o vender unidades dos veces y permite seleccionar la reserva activa más reciente por pedido. | Guardar solo el último estado perdería el historial necesario para procesar correctamente vencimientos y reintentos. |
