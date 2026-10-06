# Inicio rápido: validar reservas de inventario

## Requisitos previos

- JDK 21
- Maven

## Validación automática

Desde la raíz del repositorio, ejecutar:

```bash
mvn test
```

Resultado esperado: compilación correcta y pruebas aprobadas, incluida `InventoryServiceTest`.

## Escenarios que debe cubrir la validación

Usar un `Clock` controlable, productos registrados y un `StockAlertListener` que permita observar sus llamadas:

1. Dos reservas concurrentes para el mismo SKU nunca asignan más unidades que las disponibles; una solicitud no satisfecha falla sin reserva parcial.
2. Reenviar un pedido activo conserva una sola reserva. Al vencer, el mismo pedido puede iniciar una nueva reserva si hay existencias.
3. La reserva vence en el instante exacto `expiresAt`; sus unidades vuelven a estar disponibles.
4. `confirm(orderId)` confirma la reserva activa más reciente del pedido. Si una aprobación tardía llega después de abrir un intento nuevo, confirma ese intento activo; sin reserva activa, falla según el contrato.
5. Confirmaciones y vencimientos repetidos no cambian dos veces el total vendido ni el disponible.
6. Las tres categorías usan por omisión sus plazos y límites documentados. Una configuración alternativa válida cambia las reglas recibidas sin afectar otras instancias.
7. Una configuración sin políticas válidas para las tres categorías se rechaza al crear el servicio.
8. Un aviso se intenta al cruzar el umbral; nuevas reducciones no lo repiten. Reabastecer por encima de cinco rearma el aviso para un cruce posterior.
9. Si el listener lanza una excepción de ejecución, la mutación de inventario se conserva, la llamada al servicio no falla por ese error y no se vuelve a intentar la alerta hasta el rearme.
10. El rechazo de una línea del carrito no altera las reservas ni la disponibilidad de las demás líneas independientes.

Consulte el [contrato del servicio](contracts/inventory-service.md) y el [modelo de datos](data-model.md) para los detalles de comportamiento y configuración.
