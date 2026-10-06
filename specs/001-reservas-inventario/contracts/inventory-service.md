# Contrato: servicio de inventario

## Interfaz protegida existente

Paquete: `com.store.inventory.api` (no modificar).

- `InventoryService.registerProduct(sku, category)` registra categoría y producto.
- `InventoryService.addStock(sku, quantity)` añade unidades positivas.
- `InventoryService.reserve(orderId, sku, quantity)` reserva una línea de pedido; cantidades inválidas, límite excedido o stock insuficiente fallan sin reserva parcial.
- `InventoryService.confirm(orderId)` confirma según el contrato actual cuando hay reserva activa.
- `InventoryService.available(sku)` devuelve unidades reservables.
- `Inventory.create(Clock, StockAlertListener)` construye el servicio.
- `StockAlertListener.onLowStock(sku, availableUnits)` recibe avisos sin imponer un canal.

## Reglas observables

- Reintentar un `orderId` con reserva activa no reserva dos veces.
- El vencimiento es inclusivo: `now >= expiresAt` libera la reserva.
- Las duraciones son 15 minutos, 24 horas y 5 minutos para `STANDARD`, `PRE_ORDER` y `FLASH_SALE`; máximo dos para `FLASH_SALE`.
- El aviso se emite al alcanzar cinco o menos unidades disponibles, se suprime hasta que `addStock` supere cinco.
- Las líneas de carrito se procesan como pedidos independientes.

## Decisiones de superficie pública pendientes

FR-009 requiere una operación de aprobación que reciba `orderId` y `reservationAttemptId`, así como una forma de devolver ese ID al consumidor al crear la reserva. La API actual no ofrece ninguna de las dos. Una interfaz adicional pública y una operación explícita de creación/aprobación de intentos podría mantener los archivos existentes, pero debe definirse y aprobarse en `spec.md` antes de implementación.

FR-008 requiere registrar categorías fuera del enum cerrado `ProductCategory`. Debe aclararse en `spec.md` y en la constitución si se conserva el enum protegido y se restringe el requisito a categorías predeclaradas, o si se permite extender el API. Ningún contrato de ejecución para categorías nuevas queda definido todavía.
