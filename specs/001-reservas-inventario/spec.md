# Especificación: Reservas de inventario

**Rama de trabajo**: `001-reservas-inventario`

**Creada**: 2026-10-06

**Estado**: Borrador

**Solicitud original**: Apartar las unidades solicitadas en cada pedido, liberar las reservas vencidas, registrar como vendidas las compras pagadas y avisar cuando queden pocas existencias, sin repetir el aviso hasta que se reponga el producto. No se incluye la gestión de cuentas de cliente.

## Aclaraciones

### Sesión 2026-10-06

- P: Si una reserva vence y se vuelve a enviar el mismo pedido, ¿qué debe ocurrir? → Crear una nueva reserva con el mismo identificador de pedido si hay existencias disponibles.
- P: Si llega una aprobación de pago después de vencer una reserva y existe otra reserva vigente para el mismo pedido, ¿cuál debe confirmarse? → Confirmar la reserva vigente más reciente asociada al pedido, aunque la aprobación corresponda al intento vencido.
- P: ¿Qué categorías de producto y reglas deben estar disponibles? → Limitar esta funcionalidad a `STANDARD`, `PRE_ORDER` y `FLASH_SALE`; permitir configurar las reglas de estas tres categorías sin agregar categorías nuevas.
- P: ¿Qué debe ocurrir si no se puede entregar un aviso de existencias bajas? → Completar la operación de inventario, considerar intentado el aviso y no volver a intentarlo hasta que un reabastecimiento eleve las existencias disponibles por encima de cinco.

## Escenarios de usuario y pruebas

### Historia de usuario 1: Reservar unidades al enviar un pedido (Prioridad: P1)

Como cliente que envía un pedido de su carrito, quiero que las unidades solicitadas queden apartadas mientras pago para que no se vendan a otro cliente.

**Motivo de la prioridad**: Evita la sobreventa durante el flujo principal de compra.

**Prueba independiente**: Procesar a la vez varios pedidos del mismo producto y comprobar que las unidades reservadas no se asignan nuevamente y que el total reservado no excede las existencias.

**Escenarios de aceptación**:

1. **Dado** un producto con existencias disponibles, **cuando** un cliente solicita una cantidad válida, **entonces** esa cantidad queda reservada y deja de estar disponible para otros pedidos.
2. **Dado** que dos clientes solicitan en conjunto más unidades de las disponibles, **cuando** se procesan sus pedidos, **entonces** no se reservan más unidades que las existentes y el pedido que no puede satisfacerse recibe un rechazo claro.
3. **Dado** un pedido que se vuelve a enviar con el mismo identificador, **cuando** se recibe de nuevo mientras su reserva sigue vigente, **entonces** no se crea otra reserva ni se descuentan unidades por segunda vez.
4. **Dado** productos de distintas categorías, **cuando** se solicita una reserva, **entonces** se aplica el plazo de pago y el límite por pedido correspondiente a cada categoría.

---

### Historia de usuario 2: Liberar una reserva vencida o confirmar el pago (Prioridad: P1)

Como cliente y operador de la tienda, quiero que las reservas se liberen al vencer y que las compras pagadas queden registradas como ventas, para que las existencias disponibles y vendidas sean correctas.

**Motivo de la prioridad**: Completa el ciclo de compra y recupera las unidades de pedidos abandonados.

**Prueba independiente**: Crear una reserva y comprobar que se libera al superar su plazo; en otro pedido, aprobar el pago y comprobar que las unidades quedan contabilizadas como vendidas.

**Escenarios de aceptación**:

1. **Dada** una reserva vigente, **cuando** se aprueba el pago, **entonces** sus unidades quedan confirmadas como venta y dejan de estar disponibles.
2. **Dada** una reserva cuyo plazo venció sin aprobación de pago, **cuando** se procesa el vencimiento, **entonces** la reserva se libera y sus unidades vuelven a estar disponibles.
3. **Dado** que venció una reserva y luego se creó otra reserva vigente para el mismo pedido, **cuando** llega una aprobación de pago para ese pedido, **entonces** se confirma la reserva vigente más reciente, aunque la aprobación se haya originado en el intento vencido.
4. **Dada** una categoría `STANDARD`, `PRE_ORDER` o `FLASH_SALE`, **cuando** se crea una reserva, **entonces** el plazo es respectivamente de 15 minutos, 24 horas o 5 minutos; `FLASH_SALE` permite como máximo 2 unidades por pedido y las otras dos categorías no imponen un máximo.

---

### Historia de usuario 3: Avisar sobre existencias bajas (Prioridad: P2)

Como equipo de compras, quiero recibir un aviso cuando queden cinco unidades disponibles o menos y evitar avisos repetidos hasta reabastecer el producto, para poder reponer a tiempo sin recibir notificaciones duplicadas.

**Motivo de la prioridad**: Ayuda a reponer productos y mantiene útil la señal de existencias bajas.

**Prueba independiente**: Reducir las existencias disponibles hasta el umbral y verificar un único aviso; seguir reduciéndolas y verificar que no se repite; reabastecer por encima del umbral y volver a cruzarlo para verificar un nuevo aviso.

**Escenarios de aceptación**:

1. **Dado** un producto con más de 5 unidades disponibles y sin aviso previo pendiente de rearme, **cuando** una operación reduce sus existencias disponibles a 5 o menos, **entonces** se emite un aviso para ese producto.
2. **Dado** que ya se emitió un aviso y las existencias no han superado 5, **cuando** nuevas operaciones vuelven a dejar 5 unidades o menos, **entonces** no se repite el aviso.
3. **Dado** que un producto ya generó un aviso, **cuando** un reabastecimiento eleva sus existencias disponibles por encima de 5, **entonces** queda habilitado un nuevo aviso para el siguiente cruce del umbral.
4. **Dado** que debe emitirse un aviso, **cuando** el equipo de compras recibe la notificación, **entonces** la actualización de existencias se completa independientemente de que la notificación pueda entregarse.

### Casos límite

- Un pedido solicita cero unidades, una cantidad negativa o más unidades que las disponibles: no se reserva ninguna unidad.
- Un pedido de `FLASH_SALE` supera dos unidades: se rechaza sin reserva parcial.
- Una reserva vence exactamente al llegar a su plazo límite: se considera vencida y sus unidades pueden liberarse.
- Una aprobación de pago repetida o una liberación repetida no altera más de una vez las unidades ni el resultado final del pedido.
- Si no se puede reservar una línea del carrito, las demás líneas se procesan como pedidos independientes y no se alteran las existencias comprometidas por ellas.
- El aviso se basa en las existencias disponibles, descontando las unidades reservadas. Solo se habilita de nuevo cuando un reabastecimiento eleva las existencias disponibles por encima de 5.
- Solo se incluyen las categorías `STANDARD`, `PRE_ORDER` y `FLASH_SALE`. Las reglas de una categoría pueden configurarse sin cambiar las de las demás.
- Si no se puede entregar una notificación, el cambio de existencias se conserva; el aviso no se vuelve a intentar hasta que un reabastecimiento eleve las existencias disponibles por encima de cinco.

## Requisitos

### Requisitos funcionales

- **RF-001**: Se debe poder apartar una cantidad de un producto para un pedido.
- **RF-002**: Las unidades apartadas no se pueden asignar a otro pedido mientras la reserva siga vigente.
- **RF-003**: Una solicitud que exceda las unidades disponibles se rechaza sin apartar una cantidad parcial.
- **RF-004**: Cada producto del carrito se procesa como un pedido independiente.
- **RF-005**: Volver a enviar un pedido con una reserva vigente no debe apartar unidades por segunda vez. Si la reserva venció, se puede iniciar otra reserva con el mismo identificador de pedido cuando haya existencias.
- **RF-006**: El plazo para pagar depende de la categoría: 15 minutos para `STANDARD`, 24 horas para `PRE_ORDER` y 5 minutos para `FLASH_SALE`.
- **RF-007**: Cada pedido `FLASH_SALE` puede incluir como máximo 2 unidades; `STANDARD` y `PRE_ORDER` no tienen límite de unidades por pedido.
- **RF-008**: Las reglas de plazo y cantidad de `STANDARD`, `PRE_ORDER` y `FLASH_SALE` se pueden configurar. No se agregan otras categorías en esta funcionalidad.
- **RF-009**: Cuando se aprueba el pago de un pedido, se confirma su reserva vigente más reciente y sus unidades quedan contabilizadas como vendidas. Si la aprobación corresponde a un intento anterior vencido y existe una reserva posterior vigente, se confirma esta última.
- **RF-010**: Una reserva sin pago aprobado se libera al vencer y sus unidades vuelven a estar disponibles.
- **RF-011**: Repetir una aprobación o una liberación no cambia más de una vez las unidades ni el resultado del pedido.
- **RF-012**: Se emite un aviso asociado al producto cuando sus existencias disponibles pasan de más de 5 a 5 o menos.
- **RF-013**: No se repite el aviso mientras las existencias disponibles no vuelvan a superar 5 mediante un reabastecimiento.
- **RF-014**: La entrega de avisos no debe impedir que se complete una operación de inventario.
- **RF-015**: La funcionalidad cubre el registro de productos, el reabastecimiento, la reserva, la consulta de existencias disponibles y la confirmación de pedidos. No incluye la gestión de cuentas de cliente.
- **RF-016**: Si no se puede entregar un aviso, la operación de inventario se completa y el aviso no se vuelve a intentar hasta que un reabastecimiento eleve las existencias disponibles por encima de cinco.

### Entidades principales

- **Producto**: Artículo inventariable con categoría, existencias, unidades vendidas y unidades disponibles.
- **Pedido**: Solicitud de compra de un producto y una cantidad, identificada de manera estable para reconocer reenvíos.
- **Reserva**: Apartado temporal asociado a un pedido, con cantidad, plazo y estado: vigente, vencida/liberada o confirmada. Un pedido puede tener intentos sucesivos; una aprobación afecta a su reserva vigente más reciente.
- **Categoría de producto**: Determina el plazo de pago y, cuando corresponda, el máximo de unidades por pedido.
- **Aviso de existencias bajas**: Notificación vinculada a un producto cuando sus existencias disponibles cruzan el umbral de cinco; se puede emitir de nuevo después de reabastecer por encima del umbral.

## Criterios de éxito

### Resultados medibles

- **CE-001**: Aunque varios pedidos del mismo producto se procesen al mismo tiempo, las unidades reservadas y vendidas nunca superan las existencias recibidas.
- **CE-002**: Todas las reservas sin pago aprobado se liberan al procesarse su vencimiento y todas las reservas vigentes con pago aprobado quedan contabilizadas como ventas.
- **CE-003**: Reenviar un pedido con una reserva vigente no crea otra reserva ni aparta unidades por segunda vez.
- **CE-004**: Cada producto genera como máximo un aviso mientras sus existencias disponibles permanezcan en cinco o menos; después de reabastecer por encima de cinco, genera un nuevo aviso al volver a cruzar el umbral.
- **CE-005**: Las tres categorías aplican sus plazos y límites acordados, y configurar una categoría no modifica las reglas de las demás.

## Supuestos

- El cliente puede volver a enviar automáticamente un pedido hasta recibir respuesta. El identificador estable del pedido permite reconocer esos reintentos; después del vencimiento, ese mismo identificador puede iniciar otra reserva si hay existencias.
- El plazo de pago comienza al crear cada reserva. Al llegar al instante límite, la reserva ya está vencida.
- Las existencias disponibles se calculan restando las unidades vendidas y las unidades apartadas que siguen vigentes.
- El reabastecimiento incrementa las existencias del producto.
- La aprobación del pago llega como resultado externo asociado al pedido y confirma su reserva vigente más reciente. La conexión con un proveedor de pagos queda fuera de esta funcionalidad.
- El pedido se identifica por su identificador y no requiere una cuenta de cliente.
