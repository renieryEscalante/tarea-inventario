# Investigación: Reservas de inventario

## Plataforma y punto de entrada

- **Decisión**: Usar Java 21 y Maven; conservar `Inventory.create(Clock, StockAlertListener)` como fábrica vigente, sin agregar Spring Boot ni HTTP.
- **Fundamento**: `pom.xml` fija el compilador en Java 21 y contiene JUnit Jupiter. La constitución vigente pide Java 21, Maven y memoria, y establece que Spring Boot y controladores HTTP no son requisitos. El repositorio describe una biblioteca consumida por la aplicación móvil.
- **Alternativas consideradas**: Añadir Spring Boot o controladores. Se descartan porque no hay endpoints requeridos y ampliarían el alcance.

## Estado y concurrencia

- **Decisión**: Mantener productos, unidades vendidas, intentos de reserva y estado de alerta en memoria. Coordinar de forma atómica las mutaciones que afectan el mismo producto.
- **Fundamento**: La constitución define una sola instancia y excluye persistencia. La reserva debe comprobar y actualizar la disponibilidad en una misma sección coordinada para impedir sobreventas concurrentes.
- **Alternativas consideradas**: Base de datos o coordinación distribuida. Quedan fuera de esta iteración y serán necesarias antes de ejecutar varias instancias.

## Políticas de categoría

- **Decisión**: Representar duración y límite mediante políticas inmutables para las tres categorías existentes. Añadir `InventoryPolicies` y una sobrecarga de `Inventory.create` que reciba la configuración completa; la fábrica actual usará las reglas predeterminadas.
- **Fundamento**: La decisión registrada en `spec.md` limita esta funcionalidad al enum protegido, pero permite configurar sus reglas. Una configuración aditiva fuera del paquete `api` cumple ese requisito sin alterar sus archivos ni la firma existente.
- **Alternativas consideradas**: Dejar solo constantes internas. No permitiría configurar reglas como requiere FR-008. Modificar `ProductCategory` o `InventoryService` está prohibido por la constitución.
- **Validación propuesta**: La configuración debe incluir las tres categorías exactamente una vez; cada duración y cada límite definido debe ser positivo. `STANDARD` y `PRE_ORDER` usan límite sin máximo de forma predeterminada, y `FLASH_SALE` usa máximo dos.

## Aprobación de pagos

- **Decisión**: La aprobación se resuelve con `orderId`; cuando hay una reserva activa, se confirma la más reciente, aunque la aprobación recibida pertenezca a un intento anterior vencido.
- **Fundamento**: Esta es la decisión explícita de FR-009. Un pedido puede crear una nueva reserva tras vencer la anterior y la API protegida no distingue generaciones.
- **Alternativas consideradas**: Exigir un identificador de intento específico. Se descarta porque contradice la aclaración aceptada y exigiría exponer un dato que no está en la API protegida.
- **Comportamiento sin reserva activa**: Se conserva el contrato existente, que indica `IllegalStateException` cuando no hay reserva activa.

## Alertas y fallos del receptor

- **Decisión**: Emitir avisos mediante `StockAlertListener`, marcar el umbral como intentado y completar el cambio de inventario aunque el listener lance una excepción de ejecución. No reintentar hasta que `addStock` eleve la disponibilidad por encima de cinco.
- **Fundamento**: La regla evita que una falla de notificación haga que una operación de inventario parezca fallida o se repita. El estado de alerta permanece desacoplado del canal.
- **Alternativas consideradas**: Revertir inventario, propagar la excepción o reintentar en cada operación. La especificación rechaza las dos primeras y el reintento podría duplicar una notificación que el listener alcanzó a procesar antes del error.

## Resultado

Las decisiones funcionales de categorías, aprobación tardía y errores de alerta están resueltas en la especificación. La propuesta conserva el contrato protegido, añade solo la superficie de configuración requerida y no introduce dependencias de ejecución nuevas.
