# Modelo de datos: Reservas de inventario

## Producto inventariable

- **Identidad**: `sku` único.
- **Atributos**: categoría, unidades físicas recibidas, unidades vendidas, reservas asociadas y estado del aviso de existencias bajas.
- **Disponibilidad**: unidades físicas menos unidades vendidas y reservas activas; nunca puede ser negativa.
- **Validaciones**: registrar el producto antes de agregar stock; las adiciones deben ser positivas. La categoría pertenece al enum protegido.

## Pedido

- **Identidad**: `orderId` estable entre reintentos.
- **Contenido**: un SKU y una cantidad por pedido; cada línea del carrito se procesa de forma independiente.
- **Reintentos**: repetir un pedido con una reserva activa no reserva unidades otra vez. Tras el vencimiento se permite un intento nuevo con el mismo ID si hay stock.
- **Confirmación**: una aprobación por `orderId` selecciona la reserva activa más reciente. El servicio no distingue si el aviso de pago se originó en un intento anterior vencido.

## Intento de reserva

- **Identidad interna**: secuencia de intentos de cada pedido; el identificador de intento no se expone en el contrato protegido.
- **Atributos**: `orderId`, SKU, cantidad positiva, instante de creación, instante de vencimiento y estado.
- **Estados**: `ACTIVE`, `EXPIRED/RELEASED` o `CONFIRMED`.
- **Transiciones**: `ACTIVE → CONFIRMED` si la aprobación encuentra ese intento como reserva activa más reciente; `ACTIVE → EXPIRED/RELEASED` al llegar a `expiresAt` sin confirmación. Un estado final no se revierte.
- **Reglas**: no se reserva parcialmente; vencimiento inclusivo (`now >= expiresAt`); confirmar o liberar repetidamente no cambia dos veces las cantidades.
- **Ausencia de reserva activa**: la confirmación conserva el comportamiento definido por el API protegido y falla con `IllegalStateException`.

## Política de categoría

- **Identidad**: uno de los tres valores de `ProductCategory`.
- **Atributos**: duración positiva del plazo de pago y límite opcional de unidades por pedido, que si existe debe ser positivo.
- **Configuración**: `InventoryPolicies` contiene un mapa inmutable con exactamente una política para cada categoría. `CategoryPolicy` contiene un `Duration` positivo y un `OptionalInt` de límite; un límite vacío significa que no hay máximo. La fábrica existente usa los valores predeterminados; una sobrecarga permite pasar otra configuración.
- **Valores predeterminados**: `STANDARD` = 15 minutos/sin límite; `PRE_ORDER` = 24 horas/sin límite; `FLASH_SALE` = 5 minutos/límite 2.
- **Alcance**: no se agregan categorías fuera del enum protegido.

## Estado de alerta

- **Ámbito**: por SKU.
- **Regla**: se intenta avisar cuando una operación reduce disponibilidad desde más de cinco hasta cinco o menos y el aviso está armado. Al intentar el aviso, se desarma.
- **Rearme**: solo una operación de reabastecimiento que deje disponibilidad por encima de cinco vuelve a armarlo.
- **Fallo del receptor**: si `StockAlertListener` lanza una excepción de ejecución, no se revierte el inventario y el aviso sigue desarmado hasta el rearme por reabastecimiento.
- **Salida**: `StockAlertListener.onLowStock(sku, availableUnits)`.
