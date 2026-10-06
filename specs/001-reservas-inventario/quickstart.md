# Quickstart: validar reservas de inventario

## Prerrequisitos

- JDK 21
- Maven

## Validación

Desde la raíz del repositorio:

```bash
mvn test
```

El resultado esperado es compilación exitosa y pruebas verdes, incluida `InventoryServiceTest`.

## Escenarios de aceptación para la cobertura de la funcionalidad

La suite debe validar estos resultados mediante `InventoryService` y un `Clock` controlable:

1. Dos reservas concurrentes por el mismo stock nunca asignan más unidades de las disponibles; una solicitud no satisfecha falla sin reserva parcial.
2. Un reintento con el mismo pedido conserva una sola reserva activa. Al vencer el intento y si hay stock, el mismo pedido crea una generación nueva.
3. Una aprobación dentro del plazo confirma unidades; vencimiento inclusivo libera unidades; callbacks o liberaciones repetidas no alteran el estado dos veces.
4. Las categorías actuales usan sus plazos y límite documentados.
5. El listener recibe una sola alerta mientras disponibilidad siga en cinco o menos; un reabastecimiento por encima de cinco rearma el aviso para el siguiente cruce.
6. El fallo de reserva para una línea del carrito no cambia la disponibilidad de líneas independientes.

Consulte el [contrato](contracts/inventory-service.md) y el [modelo de datos](data-model.md). Los escenarios que requieren categorías nuevas o aprobación por ID de intento quedan condicionados a resolver los dos gates ERROR del plan.
