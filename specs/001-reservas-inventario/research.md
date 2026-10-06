# Research: Reservas de inventario

## Stack y punto de entrada

- **Decision**: Java 21 y Maven; conservar `Inventory.create(Clock, StockAlertListener)` como entrada y no añadir Spring Boot ni HTTP.
- **Rationale**: `pom.xml` fija `maven.compiler.release` en 21 y ya contiene JUnit Jupiter. La constitución IV vigente pide Java 21, Maven y memoria, y dice explícitamente que Spring Boot y controladores HTTP no son requisitos. `README.md` describe una biblioteca consumida por la aplicación móvil.
- **Alternatives considered**: Añadir Spring Boot/controladores. Descartado porque aumenta el alcance sin requisito de endpoint y contradice la constitución actual.

## Almacenamiento y concurrencia

- **Decision**: Mantener productos, cantidades vendidas, reservas activas y estado de alerta en memoria de la instancia; coordinar las mutaciones que afectan al mismo inventario de forma atómica.
- **Rationale**: El alcance declara una JVM y excluye persistencia y coordinación multi-instancia. La reserva y la disponibilidad deben actualizarse juntas para que solicitudes concurrentes no sobrevendan.
- **Alternatives considered**: Base de datos o coordinación distribuida. Fuera del alcance de esta iteración, aunque será necesaria antes de ejecutar varias instancias.

## Políticas de categoría

- **Decision**: Representar plazo y límite como una política asociada a cada categoría, en vez de mezclar esas reglas con el ciclo de reserva.
- **Rationale**: Mantiene las reglas actuales independientes y facilita añadir comportamiento cuando el tipo de categoría lo permita.
- **Alternatives considered**: `switch` fijo por las tres constantes actuales. No satisface FR-008.
- **Unresolved**: `ProductCategory` es un enum protegido con exactamente tres valores y el registro público recibe ese enum. No hay forma de suministrar una categoría estacional nueva a la firma actual. Debe acordarse si FR-008 se limita a las categorías ya declaradas con políticas reemplazables, o si se autoriza cambiar el contrato protegido.

## Identidad de aprobación de pago

- **Decision**: Toda aprobación nueva debe nombrar el intento único, junto con el pedido, para impedir aprobar accidentalmente un intento posterior.
- **Rationale**: El mismo `orderId` puede volver a reservarse tras vencer el intento previo, y callbacks tardíos deben quedar vinculados a su generación original.
- **Alternatives considered**: `confirm(orderId)` existente. No permite distinguir intentos y puede confirmar una reserva nueva tras una aprobación tardía.
- **Unresolved**: `Reservation` no expone intento y la fábrica devuelve `InventoryService`. Hay que definir la superficie pública para que el cliente obtenga y presente el ID de intento sin modificar los tipos protegidos existentes.

## Alertas de existencias

- **Decision**: Conservar la notificación mediante `StockAlertListener`; disparar al cruzar el umbral considerando stock menos reservas activas y rearmar solo al agregar stock y superar cinco.
- **Rationale**: Cumple el contrato desacoplado existente y la regla de rearme de la especificación.
- **Alternatives considered**: Integrar correo en el dominio. Descartado porque acoplaría inventario a un canal concreto.

## Resultado de la investigación

Los hechos de stack y despliegue están resueltos por el repositorio y la constitución vigente. FR-008 y FR-009 siguen como decisiones de contrato necesarias antes de implementar; no hay una solución que satisfaga literalmente ambos requisitos y mantenga intacto el API actual sin acordar una superficie adicional.
