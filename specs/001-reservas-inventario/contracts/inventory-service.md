# Contrato: servicio de inventario

## Contrato protegido existente

Paquete: `com.store.inventory.api`. Sus archivos y sus firmas permanecen intactos.

- `InventoryService.registerProduct(sku, category)` registra un producto antes de agregar stock.
- `InventoryService.addStock(sku, quantity)` agrega una cantidad positiva.
- `InventoryService.reserve(orderId, sku, quantity)` reserva una línea del pedido; rechaza cantidad inválida, límite excedido o stock insuficiente sin reserva parcial.
- `InventoryService.confirm(orderId)` confirma la reserva activa más reciente de ese pedido. Si no hay una reserva activa, lanza `IllegalStateException`.
- `InventoryService.available(sku)` devuelve unidades disponibles; para SKU desconocido devuelve cero.
- `Inventory.create(Clock, StockAlertListener)` conserva su firma y crea el servicio con las reglas predeterminadas.
- `StockAlertListener.onLowStock(sku, availableUnits)` recibe el aviso sin imponer un canal de comunicación.

## Extensión aditiva de configuración

La configuración se define fuera de `com.store.inventory.api` para no alterar el contrato protegido:

- `InventoryPolicies` contiene una política para cada valor de `ProductCategory`.
- `InventoryPolicies` conserva un mapa inmutable de `ProductCategory` a `CategoryPolicy`, con una entrada para cada categoría.
- `CategoryPolicy` contiene un `Duration` positivo y un `OptionalInt` de máximo por pedido; un máximo vacío significa que no hay límite.
- La sobrecarga `Inventory.create(Clock, StockAlertListener, InventoryPolicies)` crea el mismo `InventoryService` con las políticas recibidas.
- Una configuración inválida (categoría ausente, duración no positiva o máximo no positivo) se rechaza con `IllegalArgumentException`.
- La fábrica original usa estos valores: `STANDARD` 15 minutos y sin límite; `PRE_ORDER` 24 horas y sin límite; `FLASH_SALE` 5 minutos y máximo 2.

## Reglas observables

- Reintentar el mismo `orderId` con una reserva activa no asigna unidades nuevamente.
- Después de vencer un intento, el mismo `orderId` puede crear otra reserva si hay disponibilidad.
- La expiración es inclusiva: `now >= expiresAt` libera unidades.
- Una aprobación identificada por `orderId` confirma la reserva activa más reciente, incluso si el aviso de pago llegó por un intento anterior vencido.
- Una aprobación repetida no vende unidades más de una vez; una liberación repetida no devuelve unidades más de una vez.
- El aviso se intenta al cruzar de más de cinco a cinco o menos unidades disponibles. Tras intentarlo, no se repite hasta que `addStock` deje más de cinco unidades disponibles.
- Si el listener lanza una excepción de ejecución, el cambio de inventario se conserva, la operación termina sin propagar esa excepción y el aviso no se reintenta antes del rearme.
- Cada producto del carrito se procesa como pedido independiente.
- No se agregan endpoints ni autenticación.
