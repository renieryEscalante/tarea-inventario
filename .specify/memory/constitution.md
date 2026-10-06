<!-- Informe de impacto de sincronización
Cambio de versión: 1.1.0 → 1.2.0
Principios modificados: Ciclo de reservas, Reglas por categoría, Alertas de reabastecimiento, Contrato y arquitectura, Control del alcance; se consolidan bajo los cinco principios requeridos por el scaffold.
Secciones agregadas: Naturaleza del proyecto y reglas de reenvío; Idioma del proyecto.
Secciones eliminadas: Ninguna; se reorganizó el contenido previo según el scaffold activo.
Pendientes: TODO(RATIFICATION_DATE): confirmar fecha original de adopción.
-->
# Reservas de inventario: Constitución

## Principios fundamentales

### I. Ciclo de reservas
Cuando un cliente envíe su carrito, cada producto constituirá un pedido independiente y sus unidades quedarán reservadas para impedir que otros clientes las compren. Si el pago no se realiza antes del vencimiento, la reserva liberará las unidades. Cuando el pago se apruebe, la reserva se confirmará y las unidades quedarán vendidas.

### II. Reglas por categoría
Los plazos para pagar y los límites de unidades por pedido DEBEN depender de la categoría del producto. `STANDARD` permite unidades ilimitadas y 15 minutos para pagar; `PRE_ORDER` permite unidades ilimitadas y 24 horas para pagar; `FLASH_SALE` permite como máximo 2 unidades y 5 minutos para pagar. El diseño DEBE admitir categorías nuevas creadas por temporada sin requerir que las categorías existentes sean las únicas posibles.

### III. Alertas de reabastecimiento
Cuando un producto tenga 5 unidades disponibles o menos, el servicio DEBE emitir un aviso por `StockAlertListener`. No DEBE repetir el aviso para ese producto hasta que un reabastecimiento eleve las unidades disponibles por encima de 5. La emisión DEBE quedar desacoplada de las reglas de inventario para admitir canales de comunicación adicionales.

### IV. Contrato, stack y arquitectura
La implementación DEBE conservar el contrato público de `com.store.inventory.api`, no modificar sus archivos y mantener la firma de `Inventory.create(Clock, StockAlertListener)`. El servicio DEBE usar Java 21, Maven y almacenamiento en memoria, sin crear frontend. La arquitectura DEBE separar responsabilidades en Controller, Service, Repository y Model/DTO, con una responsabilidad clara por clase y método.

### V. Control del alcance
La implementación DEBE seguir estrictamente `spec.md` y no agregar funciones no documentadas, como pasarelas de pago o perfiles complejos. Si una solicitud contradice esta constitución o se detecta una falla lógica del dominio, el trabajo sobre código fuente DEBE detenerse hasta actualizar `spec.md`.

## Restricciones tecnológicas y del dominio

La aplicación móvil envía el carrito y puede reenviar automáticamente cada pedido hasta recibir respuesta. El servicio DEBE mantener la corrección de las reservas ante esos reintentos. Las respuestas HTTP DEBEN usar códigos semánticos. El almacenamiento permanece en memoria.

## Flujo de desarrollo y calidad

El código DEBE seguir las convenciones estándar de Java: clases en PascalCase, métodos y variables en camelCase, constantes en UPPER_SNAKE_CASE y paquetes en minúsculas organizados por dominio o funcionalidad. Los tests incluidos, en particular `InventoryServiceTest`, DEBEN pasar. Los cambios no DEBEN alterar el paquete API ni la firma del método de creación.

## Idioma del proyecto

Toda documentación generada por Spec Kit, incluidas especificaciones, planes, tareas, checklists y comentarios de documentación, DEBE escribirse en español. El código fuente DEBE mantenerse en inglés: nombres de variables, funciones, clases, archivos, endpoints y APIs DEBEN estar en inglés. Los nombres propios de tecnologías, frameworks y librerías conservarán su forma original. Si estas reglas entran en conflicto con el idioma predeterminado del agente, prevalece el español para la documentación.

## Gobernanza

Esta constitución rige las decisiones de implementación del servicio de inventario. Toda enmienda DEBE registrarse aquí. Cuando una enmienda cambie el comportamiento del dominio o el alcance, también DEBE actualizarse `spec.md`. Las revisiones DEBEN comprobar el cumplimiento de estos principios, del contrato público y de los tests existentes. La constitución usa versionado semántico: MAJOR para cambios incompatibles en principios; MINOR para principios o secciones nuevas o ampliadas de forma sustancial; PATCH para aclaraciones que no cambien requisitos. La fecha original de ratificación requiere confirmación del responsable del proyecto.

**Versión**: 1.2.0 | **Ratificada**: TODO(RATIFICATION_DATE): confirmar fecha original de adopción | **Última enmienda**: 2026-10-05
