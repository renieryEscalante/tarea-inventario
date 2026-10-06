<!-- Informe de impacto de sincronización
Cambio de versión: 1.0.0 → 1.1.0
Principios modificados (título anterior → nuevo): Inventory reservation lifecycle → Ciclo de reservas; Category-driven rules → Reglas por categoría; Replenishment alerts → Alertas de reabastecimiento; Contract and architecture → Contrato y arquitectura; Scope discipline → Control del alcance.
Secciones agregadas: Idioma del proyecto.
Secciones eliminadas: Ninguna.
Pendientes: Confirmar la fecha original de ratificación.
-->
# Constitución de Reservas de inventario

## Principios fundamentales

### I. Ciclo de reservas
Cuando un cliente envíe su carrito, cada pedido de producto reservará las unidades solicitadas para impedir que otros clientes las compren. Si el pago no se realiza antes del vencimiento, la reserva liberará las unidades. Cuando se apruebe el pago, la reserva se confirmará y las unidades quedarán vendidas.

### II. Reglas por categoría
Los plazos de pago y los límites de unidades por pedido DEBEN depender de la categoría del producto y permitir nuevas categorías de temporada. `STANDARD` permite unidades ilimitadas y 15 minutos para pagar; `PRE_ORDER` permite unidades ilimitadas y 24 horas para pagar; `FLASH_SALE` permite como máximo 2 unidades y 5 minutos para pagar.

### III. Alertas de reabastecimiento
Cuando el inventario disponible de un producto llegue a 5 unidades o menos, el servicio DEBE emitir un aviso mediante `StockAlertListener`. No DEBE repetir el aviso para ese producto hasta que el reabastecimiento eleve el inventario disponible por encima de 5. La entrega DEBE permanecer desacoplada de las reglas de inventario para permitir agregar otros canales de comunicación.

### IV. Contrato y arquitectura
La implementación DEBE conservar el contrato público de `com.store.inventory.api` y la firma de `Inventory.create(Clock, StockAlertListener)`. No DEBE modificar archivos del paquete API. El servicio DEBE usar Java 21, Maven y almacenamiento en memoria, y separar responsabilidades en capas Controller, Service, Repository y Model/DTO. Este servicio no incluye frontend.

### V. Control del alcance
La implementación DEBE seguir `spec.md` y no DEBE agregar funciones no documentadas, como pasarelas de pago o perfiles complejos. Si una solicitud contradice esta constitución o presenta una falla lógica del dominio, el trabajo sobre código fuente DEBE detenerse hasta actualizar `spec.md`.

## Restricciones tecnológicas y del dominio

El servicio de inventario se consume mediante el contrato existente. Cada producto enviado en el carrito constituye un pedido independiente. La aplicación móvil puede reenviar un pedido hasta recibir respuesta; el servicio DEBE mantener la corrección de las reservas ante esos reintentos. El comportamiento expuesto por HTTP DEBE usar códigos semánticos, incluidos 400 para solicitudes inválidas, 401 para solicitudes no autenticadas y 409 para conflictos de reserva. El almacenamiento permanece en memoria.

## Flujo de desarrollo y calidad

El código DEBE seguir las convenciones estándar de Java: clases en PascalCase, métodos y variables en camelCase, constantes en UPPER_SNAKE_CASE y paquetes en minúsculas organizados por dominio o funcionalidad. Cada clase y método DEBE tener una responsabilidad clara y limitada. Los tests existentes de `InventoryServiceTest` DEBEN pasar. Los cambios no DEBEN alterar archivos de `com.store.inventory.api` ni la firma del método de creación.

## Idioma del proyecto

Toda documentación generada por Spec Kit, incluidas especificaciones, planes, tareas, checklists y comentarios de documentación, DEBE escribirse en español. El código fuente DEBE mantenerse en inglés: nombres de variables, funciones, clases, archivos, endpoints y APIs DEBEN estar en inglés. Los nombres propios de tecnologías, frameworks y librerías conservarán su forma original. Si estas reglas entran en conflicto con el idioma predeterminado del agente, prevalece el español para la documentación.

## Gobernanza

Esta constitución rige las decisiones de implementación del servicio de inventario. Toda enmienda DEBE registrarse aquí y reflejarse en `spec.md` cuando cambie el comportamiento del dominio o el alcance del proyecto. Las revisiones DEBEN comprobar el cumplimiento de estos principios, del contrato público y de los tests existentes. La constitución usa versionado semántico: MAJOR para cambios incompatibles en principios; MINOR para principios o secciones nuevas o ampliadas de forma sustancial; PATCH para aclaraciones que no cambien requisitos. La fecha de ratificación original es desconocida y requiere confirmación del responsable del proyecto.

**Versión**: 1.1.0 | **Ratificada**: TODO(RATIFICATION_DATE): confirmar fecha original de adopción | **Última enmienda**: 2026-10-05
