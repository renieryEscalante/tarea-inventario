# Feature Specification: Reservas de inventario

**Feature Branch**: `001-reservas-inventario`

**Created**: 2026-10-06

**Status**: Draft

**Input**: User description: "Implementar InventoryService para reservar las unidades del carrito, liberar reservas vencidas, confirmar compras pagadas y emitir una alerta de pocas existencias sin repetirla hasta el reabastecimiento. No autenticar usuarios ni crear endpoints."

## User Scenarios & Testing

### User Story 1 - Reservar unidades al enviar un pedido (Priority: P1)

Como cliente que envía un pedido de su carrito, quiero que las unidades solicitadas queden apartadas mientras pago para que no se vendan a otro cliente.

**Why this priority**: Evita la sobreventa durante el flujo principal de compra.

**Independent Test**: Enviar pedidos concurrentes para el mismo producto y comprobar que las unidades ya reservadas no pueden asignarse nuevamente.

**Acceptance Scenarios**:

1. **Given** un producto con existencias disponibles, **When** un cliente solicita una cantidad válida, **Then** esa cantidad queda reservada y deja de estar disponible para otros pedidos.
2. **Given** dos clientes solicitan unidades cuya suma excede las disponibles, **When** ambos pedidos se procesan, **Then** no se reservan más unidades que las disponibles y el pedido que no puede satisfacerse recibe un resultado de rechazo claro.
3. **Given** una solicitud que se reenvía con la misma identidad de pedido, **When** el servicio la recibe de nuevo, **Then** no crea una segunda reserva ni duplica la reducción de unidades disponibles.
4. **Given** productos de distintas categorías, **When** se solicita una reserva, **Then** se aplica el plazo de pago y el límite por pedido correspondiente a cada categoría.

---

### User Story 2 - Liberar una reserva vencida o confirmar el pago (Priority: P1)

Como cliente y operador de la tienda, quiero que las reservas se liberen al vencer y se confirmen cuando el pago se apruebe, para que el inventario represente correctamente las unidades disponibles y vendidas.

**Why this priority**: Completa el ciclo de vida de la reserva y recupera unidades de pedidos abandonados.

**Independent Test**: Crear una reserva, avanzar el tiempo más allá del vencimiento y comprobar su liberación; en otro caso aprobar el pago y comprobar que la reserva queda confirmada como venta.

**Acceptance Scenarios**:

1. **Given** una reserva aún vigente, **When** se aprueba el pago, **Then** la reserva se confirma y sus unidades dejan de formar parte del inventario disponible.
2. **Given** una reserva cuyo plazo de pago ha vencido sin aprobación, **When** el servicio procesa el vencimiento, **Then** la reserva se libera y sus unidades vuelven a estar disponibles.
3. **Given** una reserva liberada por vencimiento, **When** llega posteriormente una aprobación de pago, **Then** el servicio no confirma esa reserva vencida ni descuenta unidades que ya se hayan reasignado.
4. **Given** una categoría `STANDARD`, `PRE_ORDER` o `FLASH_SALE`, **When** se crea una reserva, **Then** su plazo es respectivamente 15 minutos, 24 horas o 5 minutos; `FLASH_SALE` limita la solicitud a 2 unidades y las otras dos categorías no imponen límite por pedido.

---

### User Story 3 - Avisar sobre existencias bajas (Priority: P2)

Como equipo de compras, quiero recibir un aviso cuando las unidades disponibles de un producto lleguen a cinco o menos, y evitar avisos repetidos hasta que se reabastezca, para poder reponer a tiempo sin ruido duplicado.

**Why this priority**: Ayuda a reponer productos y mantiene útil la señal de inventario bajo.

**Independent Test**: Reducir las existencias disponibles hasta el umbral y verificar un único aviso; continuar reduciéndolas y verificar que no se repite; reabastecer por encima del umbral y volver a cruzarlo para verificar un nuevo aviso.

**Acceptance Scenarios**:

1. **Given** un producto con más de 5 unidades disponibles y sin aviso activo, **When** una operación reduce sus unidades disponibles a 5 o menos, **Then** se emite un aviso para ese producto.
2. **Given** que ya se emitió un aviso y las existencias no han superado 5, **When** nuevas operaciones vuelven a dejar 5 o menos unidades, **Then** no se repite el aviso.
3. **Given** que un producto ya generó un aviso, **When** un reabastecimiento eleva sus unidades disponibles por encima de 5, **Then** se restablece la posibilidad de emitir un aviso en el siguiente cruce al umbral.
4. **Given** que se debe emitir un aviso, **When** el servicio notifica al receptor de alertas, **Then** la regla de inventario no depende de un canal de comunicación específico.

### Edge Cases

- Un pedido solicita cero unidades, una cantidad negativa o más unidades que las disponibles: no debe reservarse ninguna unidad.
- Un pedido de `FLASH_SALE` supera dos unidades: debe rechazarse sin reserva parcial.
- La reserva vence exactamente al alcanzar su instante límite: se considera vencida y sus unidades pueden liberarse.
- Una aprobación de pago repetida o una liberación repetida no debe alterar dos veces las existencias ni el estado final.
- Si una línea del carrito no puede reservarse, las demás líneas se siguen procesando como pedidos independientes y no se altera la disponibilidad comprometida por cada una.
- Una alerta corresponde a existencias disponibles, descontando las unidades reservadas; el aviso solo se rearma cuando un reabastecimiento supera el umbral de 5 unidades disponibles.
- Una categoría nueva debe poder definir sus reglas de límite y vencimiento sin invalidar las categorías ya existentes.

## Requirements

### Functional Requirements

- **FR-001**: El servicio DEBE permitir solicitar una reserva de inventario para un producto y una cantidad asociada a un pedido.
- **FR-002**: El servicio DEBE impedir que las unidades reservadas se asignen a otro pedido mientras la reserva siga vigente.
- **FR-003**: El servicio DEBE rechazar una solicitud que exceda las unidades disponibles, sin crear una reserva parcial.
- **FR-004**: El servicio DEBE procesar cada producto del carrito como un pedido independiente.
- **FR-005**: El servicio DEBE reconocer reenvíos del mismo pedido y evitar reservas duplicadas.
- **FR-006**: El servicio DEBE establecer vencimiento según la categoría del producto: 15 minutos para `STANDARD`, 24 horas para `PRE_ORDER` y 5 minutos para `FLASH_SALE`.
- **FR-007**: El servicio DEBE limitar los pedidos `FLASH_SALE` a 2 unidades; `STANDARD` y `PRE_ORDER` no tienen límite de unidades por pedido.
- **FR-008**: El servicio DEBE permitir agregar categorías de producto con sus propias reglas de reserva, sin restringir el catálogo a las categorías enumeradas en esta especificación.
- **FR-009**: El servicio DEBE confirmar una reserva vigente cuando se apruebe su pago y mantener sus unidades como vendidas, no disponibles.
- **FR-010**: El servicio DEBE liberar una reserva al vencer sin pago aprobado y devolver sus unidades al inventario disponible.
- **FR-011**: El servicio DEBE evitar que aprobaciones o liberaciones repetidas cambien más de una vez las unidades o el estado de la reserva.
- **FR-012**: El servicio DEBE emitir un aviso asociado al producto cuando sus unidades disponibles pasen a 5 o menos.
- **FR-013**: El servicio NO DEBE repetir el aviso del producto mientras las unidades disponibles no hayan vuelto a superar 5 por reabastecimiento.
- **FR-014**: El servicio DEBE permitir que los avisos sean recibidos por un mecanismo desacoplado de un canal de comunicación concreto.
- **FR-015**: La funcionalidad DEBE estar disponible mediante la interfaz de servicio InventoryService; no se crearán endpoints ni se requerirá autenticación de usuarios.

### Key Entities

- **Producto**: Artículo inventariable con categoría, existencias y unidades disponibles.
- **Pedido**: Solicitud de compra de un producto y una cantidad; mantiene identidad estable para reconocer reenvíos.
- **Reserva**: Apartado temporal asociado a un pedido, con cantidad, vencimiento y estado vigente, vencido/liberado o confirmado.
- **Categoría de producto**: Define el plazo de pago y, cuando corresponda, el límite de unidades por pedido.
- **Aviso de existencias bajas**: Señal vinculada a un producto cuando sus unidades disponibles cruzan el umbral de cinco; su emisión se rearma tras reabastecer por encima del umbral.

## Success Criteria

### Measurable Outcomes

- **SC-001**: En todas las pruebas de solicitudes concurrentes, las unidades reservadas o vendidas nunca superan las existencias del producto.
- **SC-002**: El 100% de las reservas sin pago se libera al procesar su vencimiento, y el 100% de las reservas con pago aprobado vigente queda confirmada.
- **SC-003**: El reenvío de una solicitud de pedido no incrementa en más de una reserva la asignación de unidades para ese pedido.
- **SC-004**: Cada producto emite como máximo un aviso mientras sus existencias disponibles permanezcan en cinco o menos; tras reabastecer por encima de cinco, vuelve a emitir un aviso en el siguiente cruce.
- **SC-005**: Las reglas de los tres tipos de categoría definidos se aplican correctamente en todos sus casos de aceptación, y una categoría adicional puede declararse con reglas propias sin cambiar esos resultados.

## Assumptions

- El identificador estable del pedido se proporciona al servicio para reconocer reintentos de la aplicación móvil.
- Los plazos se cuentan desde la creación de cada reserva y el instante exacto de vencimiento ya se considera vencido.
- El cálculo de unidades disponibles descuenta las reservas vigentes y las unidades vendidas.
- El reabastecimiento se representa como una operación que incrementa las existencias del producto.
- La aprobación del pago llega al servicio como un resultado externo; integrar una pasarela de pago no forma parte de esta iteración.
- El servicio opera sin identidad autenticada de usuario; el pedido se identifica por su identificador, no por una cuenta.

