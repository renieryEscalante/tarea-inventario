# Modelo de datos: Reservas de inventario

## Producto inventariable

- **Identidad**: `sku` único.
- **Atributos**: categoría (`ProductCategory`), unidades físicas agregadas, unidades vendidas, intentos de reserva asociados y estado de alerta.
- **Regla de disponibilidad**: `stock físico - unidades vendidas - suma de reservas activas`; nunca menor que cero.
- **Validación**: registrar antes de agregar stock; adiciones deben ser positivas. Una categoría nueva requiere resolver FR-008 y el límite del enum protegido.

## Pedido

- **Identidad**: `orderId`, estable entre reintentos.
- **Relación**: un pedido en esta interfaz corresponde a un solo SKU y cantidad; cada línea de carrito es independiente.
- **Idempotencia**: reenvío durante un intento activo devuelve ese intento sin reservar unidades otra vez. Tras vencer, puede abrir otro intento con el mismo pedido si hay stock.

## Intento de reserva

- **Identidad**: `reservationAttemptId` único y distinto entre generaciones del mismo pedido.
- **Atributos**: `orderId`, SKU, cantidad positiva, instante de creación, instante de vencimiento y estado.
- **Estados**: `ACTIVE`, `EXPIRED/RELEASED`, `CONFIRMED`.
- **Transiciones**: `ACTIVE → CONFIRMED` si se aprueba antes del vencimiento; `ACTIVE → EXPIRED/RELEASED` al llegar a `expiresAt` sin aprobación. Los estados finales no se revierten.
- **Validaciones**: no reservar parcialmente; aplicar el máximo de categoría si existe; en `now >= expiresAt` se considera vencida. Confirmación repetida o liberación repetida no cambia cantidades por segunda vez.
- **Pendiente**: la interfaz protegida `Reservation` no incluye `reservationAttemptId`; la extensión que lo expone debe acordarse.

## Política de categoría

- **Atributos**: duración de pago y máximo opcional de unidades por pedido.
- **Valores actuales**: `STANDARD` = 15 minutos/sin máximo; `PRE_ORDER` = 24 horas/sin máximo; `FLASH_SALE` = 5 minutos/máximo 2.
- **Pendiente**: el enum protegido impide representar nuevas categorías, aunque el requisito pide admitirlas.

## Estado de alerta

- **Ámbito**: por SKU.
- **Regla**: emitir cuando disponibilidad pasa a cinco o menos y el aviso está armado; después de emitir queda desarmado. Solo una operación `addStock` que deje más de cinco unidades disponibles lo rearma.
- **Salida**: `StockAlertListener.onLowStock(sku, availableUnits)`.
