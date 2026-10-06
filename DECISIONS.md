# Decisiones de implementación: Reservas de inventario

## Supuestos

- Los productos, las ventas y los intentos de reserva viven en memoria durante la ejecución del proceso. Reiniciar el proceso elimina ese estado.
- Cada línea del carrito se maneja como un pedido independiente y usa un identificador estable para reconocer reenvíos.
- Reenviar un pedido con una reserva vigente devuelve esa misma reserva. Una vez vencida, se puede crear un nuevo intento con el mismo identificador si hay existencias.
- La aprobación se resuelve por el identificador del pedido y confirma el intento vigente más reciente. La interfaz protegida no distingue qué intento originó una aprobación tardía.
- El vencimiento inclusivo se procesa de forma diferida al realizar una operación del inventario; no hay una tarea periódica de limpieza.
- Un aviso se considera intentado antes de llamar al receptor. Si este lanza una excepción de ejecución, se conserva la mutación y no se reintenta hasta que un reabastecimiento eleve las existencias disponibles por encima de cinco.
- Las operaciones del servicio se coordinan dentro de una única instancia para mantener atómicas las comprobaciones y cambios de inventario.

## Fuera del alcance actual

- Persistencia de productos, ventas o reservas entre reinicios.
- Coordinación entre varias instancias del servicio.
- Integración con proveedores de pago, cuentas de cliente o interfaces web.
- Garantía de entrega de avisos cuando el receptor falla; el contrato actual registra el intento y no reintenta antes del reabastecimiento.

## Cambios necesarios antes de producción

- Añadir almacenamiento duradero y transacciones que preserven las invariantes de stock.
- Sustituir la coordinación local por una estrategia segura para varias instancias.
- Definir la retención y limpieza de intentos vencidos y confirmados.
- Revisar con el proveedor de pagos cómo distinguir generaciones de reserva para evitar que una aprobación tardía confirme un intento distinto al pagado.
- Si el negocio necesita entrega garantizada de avisos, incorporar una cola u outbox duradera y acordar una política de reintentos e idempotencia.
- Añadir observabilidad operativa para reservas, vencimientos, ventas, rechazos y avisos fallidos.
