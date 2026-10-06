# Tareas: Reservas de inventario

**Entradas**: Documentos de diseño de `specs/001-reservas-inventario/`

**Requisitos previos**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/inventory-service.md`

**Pruebas**: Agregar pruebas enfocadas a los escenarios de aceptación e invariantes descritos en `spec.md` y `quickstart.md`, dentro del archivo JUnit existente `InventoryServiceTest.java`.

**Organización**: Las tareas se agrupan por historia de usuario para permitir implementar y validar cada historia por separado.

## Fase 1: Preparación

**Propósito**: Confirmar la configuración actual de Maven y el contrato protegido del API.

- [X] T001 Verificar la configuración de Java 21 y JUnit Jupiter 5.10.2 en `pom.xml`
- [X] T002 Identificar los contratos del servicio y los archivos protegidos existentes bajo `src/main/java/com/store/inventory/api/`

---

## Fase 2: Fundamentos

**Propósito**: Establecer el estado compartido del inventario en memoria y la coordinación atómica antes de implementar las historias.

- [X] T003 [P] Crear `ProductState.java` y `ReservationAttempt.java` en `src/main/java/com/store/inventory/internal/model/`
- [X] T004 [P] Crear la coordinación por producto y el cálculo de existencias disponibles en `src/main/java/com/store/inventory/internal/service/ProductInventory.java`
- [X] T005 Conectar la construcción del servicio interno desde `src/main/java/com/store/inventory/Inventory.java` y conservar `Inventory.create(Clock, StockAlertListener)`

**Punto de control**: El estado y la construcción compartidos están listos. No modificar archivos de `src/main/java/com/store/inventory/api/`.

---

## Fase 3: Historia de usuario 1: Reservar unidades al enviar un pedido (Prioridad: P1) 🎯 MVP

**Objetivo**: Reservar existencias de forma atómica, rechazar solicitudes no satisfechas sin reserva parcial y procesar reintentos de forma segura.

**Prueba independiente**: Enviar solicitudes concurrentes para un mismo SKU y comprobar que las reservas nunca excedan las existencias. Verificar cantidades inválidas, falta de existencias, reintento de un pedido activo, reglas por categoría y procesamiento independiente de cada línea del carrito.

### Pruebas de la historia de usuario 1

- [X] T006 [US1] Agregar pruebas de reserva, falta de existencias, cantidad inválida y reintento de pedido activo en `src/test/java/com/store/inventory/InventoryServiceTest.java`
- [X] T007 [US1] Agregar pruebas de reservas concurrentes y procesamiento independiente de líneas del carrito en `src/test/java/com/store/inventory/InventoryServiceTest.java`
- [X] T008 [US1] Agregar pruebas de valores predeterminados por categoría y validación de políticas configurables en `src/test/java/com/store/inventory/InventoryServiceTest.java`

### Implementación de la historia de usuario 1

- [X] T009 [P] [US1] Implementar `CategoryPolicy` con una `Duration` positiva y un máximo `OptionalInt` que, si está presente, debe ser positivo en `src/main/java/com/store/inventory/CategoryPolicy.java`
- [X] T010 [US1] Implementar `InventoryPolicies` como mapa inmutable con exactamente una política para cada `ProductCategory` en `src/main/java/com/store/inventory/InventoryPolicies.java`
- [X] T011 [US1] Implementar las políticas predeterminadas (`STANDARD`: 15 minutos/sin máximo; `PRE_ORDER`: 24 horas/sin máximo; `FLASH_SALE`: 5 minutos/máximo 2) y la sobrecarga de fábrica configurable en `src/main/java/com/store/inventory/Inventory.java`
- [X] T012 [US1] Implementar el registro de productos, la adición de existencias positivas, la consulta de existencias disponibles y la reserva atómica completa con reintentos de pedidos activos en `src/main/java/com/store/inventory/internal/service/InventoryServiceImpl.java`
- [X] T013 [US1] Rechazar solicitudes que excedan el límite de la categoría o las existencias disponibles, sin crear reservas parciales, en `src/main/java/com/store/inventory/internal/service/InventoryServiceImpl.java`

**Punto de control**: La historia 1 permite reservar existencias por separado y aplica las duraciones y límites configurados para cada categoría.

---

## Fase 4: Historia de usuario 2: Liberar una reserva vencida o confirmar el pago (Prioridad: P1)

**Objetivo**: Vencer las reservas al llegar a su límite inclusivo, permitir otro intento con el mismo ID después del vencimiento y confirmar una sola vez el intento activo más reciente.

**Prueba independiente**: Usar un `Clock` controlable para verificar la confirmación de una reserva vigente, su liberación en el instante exacto de vencimiento, un nuevo intento con el mismo ID, una aprobación tardía que selecciona el intento activo más reciente y la idempotencia de operaciones repetidas.

### Pruebas de la historia de usuario 2

- [X] T014 [US2] Agregar pruebas de liberación en el instante exacto de vencimiento, nuevo intento con el mismo pedido y liberación repetida en `src/test/java/com/store/inventory/InventoryServiceTest.java`
- [X] T015 [US2] Agregar pruebas de confirmación, aprobación tardía del intento activo más reciente, ausencia de reserva activa y confirmación repetida en `src/test/java/com/store/inventory/InventoryServiceTest.java`

### Implementación de la historia de usuario 2

- [X] T016 [US2] Implementar el vencimiento inclusivo (`now >= expiresAt`) y liberar las unidades reservadas una sola vez en `src/main/java/com/store/inventory/internal/service/InventoryServiceImpl.java`
- [X] T017 [US2] Permitir un nuevo intento de reserva con el mismo `orderId` después de vencer el intento anterior en `src/main/java/com/store/inventory/internal/service/InventoryServiceImpl.java`
- [X] T018 [US2] Implementar `confirm(orderId)` para confirmar el intento activo más reciente, contabilizar sus unidades vendidas una sola vez y lanzar `IllegalStateException` si no hay un intento activo en `src/main/java/com/store/inventory/internal/service/InventoryServiceImpl.java`

**Punto de control**: El vencimiento y la confirmación preservan las invariantes de existencias ante reintentos.

---

## Fase 5: Historia de usuario 3: Avisar sobre existencias bajas (Prioridad: P2)

**Objetivo**: Notificar una vez cuando las existencias disponibles lleguen a cinco o menos, rearmar el aviso solo después de reabastecer por encima de cinco y aislar los fallos del receptor.

**Prueba independiente**: Cruzar el umbral mediante operaciones que reduzcan existencias y verificar que se envía un solo aviso. Reabastecer por encima de cinco y volver a cruzar el umbral para verificar un nuevo aviso. Hacer que el listener lance una excepción y comprobar que el cambio se conserva y no se reintenta antes del rearme.

### Pruebas de la historia de usuario 3

- [X] T019 [US3] Agregar pruebas de cruce del umbral, supresión de avisos repetidos y rearme por reabastecimiento en `src/test/java/com/store/inventory/InventoryServiceTest.java`
- [X] T020 [US3] Agregar pruebas del comportamiento ante una excepción de ejecución del listener en `src/test/java/com/store/inventory/InventoryServiceTest.java`

### Implementación de la historia de usuario 3

- [X] T021 [US3] Implementar el estado de aviso armado por SKU y detectar cruces de disponibilidad cuando una reserva reduce las existencias disponibles en `src/main/java/com/store/inventory/internal/service/InventoryServiceImpl.java`
- [X] T022 [US3] Rearmar el aviso de existencias bajas solo cuando `addStock` eleve las existencias disponibles por encima de cinco en `src/main/java/com/store/inventory/internal/service/InventoryServiceImpl.java`
- [X] T023 [US3] Aislar las excepciones de ejecución del listener después de marcar el aviso como intentado y conservar la mutación de inventario completada en `src/main/java/com/store/inventory/internal/service/InventoryServiceImpl.java`

**Punto de control**: Los avisos usan `StockAlertListener` y su comportamiento se mantiene estable si el listener falla.

---

## Fase 6: Pulido y aspectos transversales

**Propósito**: Verificar el incremento completo frente al comportamiento documentado y las restricciones del repositorio.

- [X] T024 [P] Revisar el API público resultante y confirmar que no se modificó ningún archivo bajo `src/main/java/com/store/inventory/api/`
- [X] T025 Ejecutar el comando de validación de Maven `mvn test` desde la raíz del repositorio
- [X] T026 Revisar la implementación frente a `specs/001-reservas-inventario/quickstart.md` y actualizar `specs/001-reservas-inventario/quickstart.md` solo si se necesita corregir algún detalle
- [X] T027 Documentar los supuestos, exclusiones y preparativos previos a producción en `DECISIONS.md`, según las instrucciones de `README.md`
- [X] T028 Crear reglas de exclusión para los artefactos generados por Maven y Java en `.gitignore`

---

## Dependencias y orden de ejecución

### Dependencias entre fases

- **Preparación (Fase 1)**: Sin dependencias; verifica la base existente.
- **Fundamentos (Fase 2)**: Depende de Preparación y bloquea todas las historias.
- **Historias de usuario (Fases 3–5)**: Dependen de Fundamentos. US2 depende del estado y comportamiento de reservas de US1. US3 depende de las mutaciones de existencias y su cálculo de US1. El orden de implementación es US1 → US2 → US3.
- **Pulido (Fase 6)**: Depende de que se completen todas las historias previstas.

### Dependencias entre historias

- **US1 (P1)**: Puede comenzar después de Fundamentos; no depende de otras historias.
- **US2 (P1)**: Comienza después de US1 porque amplía el ciclo de vida de los intentos de reserva.
- **US3 (P2)**: Comienza después de US1 porque observa cambios de existencias y disponibilidad; la integración también cubre los cambios producidos por la confirmación de US2.

### Oportunidades de trabajo en paralelo

- T003 y T004 pueden avanzar en paralelo si se acuerdan sus interfaces internas.
- T009 debe preceder a T010; T011 depende de ambos tipos de política.
- Las tareas T006–T008 comparten el mismo archivo de pruebas y deben editarse en secuencia para evitar conflictos.
- Las tareas T014–T015 y T019–T020 comparten el mismo archivo de pruebas; coordinar las ediciones o realizarlas en secuencia.
- US2 y US3 requieren que exista primero el servicio central de inventario de US1.

## Ejemplo de paralelismo: historia de usuario 1

```text
T003: Crear ProductState.java y ReservationAttempt.java en src/main/java/com/store/inventory/internal/model/
T004: Crear ProductInventory.java en src/main/java/com/store/inventory/internal/service/
```

## Estrategia de implementación

### Primero el MVP (solo historia de usuario 1)

1. Completar Preparación y Fundamentos.
2. Completar las políticas, reservas, reintentos y concurrencia de US1.
3. Validar US1 de forma independiente con sus escenarios de `InventoryServiceTest.java`.
4. Entregar el MVP de reservas antes de añadir vencimientos, confirmaciones y avisos.

### Entrega incremental

1. Agregar US2 para completar el ciclo de vida de la reserva y validarlo por separado.
2. Agregar US3 para los avisos de existencias bajas y el manejo de fallos del listener.
3. Completar la revisión transversal y ejecutar la validación Maven indicada en la guía rápida.

## Notas

- `[P]` indica tareas que pueden ejecutarse en paralelo porque usan archivos distintos y no dependen de tareas incompletas.
- Los archivos del API protegido bajo `src/main/java/com/store/inventory/api/` deben permanecer sin cambios.
- No se incluyen endpoints HTTP, autenticación ni integración con pasarelas de pago.
