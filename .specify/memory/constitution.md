<!-- Informe de impacto de sincronización
Cambio de versión: 1.2.2 → 1.2.3
Principios modificados: IV. Contrato, stack y arquitectura; se elimina la exigencia de Spring Boot y de controladores.
Secciones agregadas: ninguna.
Secciones eliminadas: ninguna.
Pendientes: TODO(RATIFICATION_DATE): confirmar fecha original de adopción.
-->
<!-- Informe de impacto de sincronización
Cambio de versión: 1.2.3 → 2.0.0
Principios modificados: II. Reglas por categoría; esta funcionalidad queda limitada a las categorías declaradas en el API protegido.
Secciones agregadas: ninguna.
Secciones eliminadas: requisito de incorporar categorías estacionales nuevas en esta funcionalidad.
Pendientes: TODO(RATIFICATION_DATE): confirmar fecha original de adopción.
-->
# Reservas de inventario: Constitución

## Principios fundamentales

### I. Ciclo de reservas
Cuando un cliente envíe su carrito, cada producto constituirá un pedido independiente y sus
unidades quedarán reservadas para impedir que otros clientes las compren. Si el pago no se realiza
antes del vencimiento, la reserva liberará las unidades. Cuando el pago se apruebe, la reserva se
confirmará y las unidades quedarán vendidas.

### II. Reglas por categoría
Los plazos para pagar y los límites de unidades por pedido DEBEN depender de la categoría del
producto. `STANDARD` permite unidades ilimitadas y 15 minutos para pagar; `PRE_ORDER` permite
unidades ilimitadas y 24 horas para pagar; `FLASH_SALE` permite como máximo 2 unidades y 5 minutos
para pagar. Esta funcionalidad queda limitada a las categorías declaradas en el contrato protegido;
admitir categorías nuevas creadas por temporada requiere una actualización futura del alcance y del
contrato público.

### III. Alertas de reabastecimiento
Cuando un producto tenga 5 unidades disponibles o menos, el servicio DEBE emitir un aviso por
`StockAlertListener`. No DEBE repetir el aviso para ese producto hasta que un reabastecimiento
eleve las unidades disponibles por encima de 5. La emisión DEBE quedar desacoplada de las reglas
de inventario para admitir canales de comunicación adicionales.

### IV. Contrato, stack y arquitectura
La implementación DEBE conservar el contrato público de `com.store.inventory.api`, no modificar
sus archivos y mantener la firma de `Inventory.create(Clock, StockAlertListener)`. El servicio
DEBE usar Java 21, Maven y almacenamiento en memoria, sin crear frontend. Spring Boot y los
controladores HTTP no son requisitos para este proyecto. La estructura interna DEBE ser sencilla y
separar responsabilidades cuando el dominio lo justifique, con una responsabilidad clara por clase
y método; no DEBE imponerse una arquitectura por capas que no sea necesaria para cumplir `spec.md`.

### V. Control del alcance
La implementación DEBE seguir estrictamente `spec.md` y no agregar funciones no documentadas,
como pasarelas de pago o perfiles complejos. Si una solicitud contradice esta constitución o se
detecta una falla lógica del dominio, el trabajo sobre código fuente DEBE detenerse hasta actualizar
`spec.md`.

## Restricciones tecnológicas y del dominio

La aplicación móvil envía el carrito y puede reenviar automáticamente cada pedido hasta recibir
respuesta. El servicio DEBE mantener la corrección de las reservas ante esos reintentos. El
almacenamiento permanece en memoria. El manejo de HTTP solo aplica si `spec.md` requiere una
interfaz HTTP.

## Flujo de desarrollo y calidad

El código DEBE seguir las convenciones estándar de Java: clases en PascalCase, métodos y variables
en camelCase, constantes en UPPER_SNAKE_CASE y paquetes en minúsculas organizados por dominio o
funcionalidad. Los tests incluidos, en particular `InventoryServiceTest`, DEBEN pasar. Los cambios
no DEBEN alterar el paquete API ni la firma del método de creación.

## Gobernanza

Esta constitución rige las decisiones de implementación del servicio de inventario. Toda enmienda
DEBE registrarse aquí. Cuando una enmienda cambie el comportamiento del dominio o el alcance,
también DEBE actualizarse `spec.md`. Las revisiones DEBEN comprobar el cumplimiento de estos
principios, del contrato público y de los tests existentes. La constitución usa versionado
semántico: MAJOR para cambios incompatibles en principios; MINOR para principios o secciones nuevas
o ampliadas de forma sustancial; PATCH para aclaraciones que no cambien requisitos. La fecha
original de ratificación requiere confirmación del responsable del proyecto.

**Versión**: 2.0.0 | **Ratificada**: TODO(RATIFICATION_DATE): confirmar fecha original de adopción | **Última enmienda**: 2026-10-06
