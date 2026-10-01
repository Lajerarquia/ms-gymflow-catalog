# ms-gymflow-catalog

Catálogo de GymFlow: clases (en la API, `services`), salas (`rooms`) y cupos. Persiste en Amazon RDS PostgreSQL.
No está expuesto a internet: lo llaman el BFF (rutas públicas `/api/catalog/...`) y ms-gymflow-reservations
(rutas internas de cupo).

## API pública (a través del BFF)

| Método y ruta | Descripción |
|---|---|
| `GET /api/catalog/services?from=&to=&roomId=` | Lista clases, ordenadas por inicio. `from`/`to` en ISO-8601 |
| `GET /api/catalog/services/{id}` | Detalle de una clase |
| `POST /api/catalog/services` | Crea una clase (201) |
| `PUT /api/catalog/services/{id}` | Edita la clase, incluidos plan y cupo |
| `DELETE /api/catalog/services/{id}` | Elimina (204). 409 si tiene reservas confirmadas |
| `GET/POST /api/catalog/rooms`, `GET/PUT/DELETE /api/catalog/rooms/{id}` | Salas |

Clase:
```json
{
  "id": 1, "name": "Spinning 45", "description": "…", "instructor": "Camila Rojas",
  "roomId": 1, "roomName": "Sala Spinning", "branch": "Providencia",
  "startsAt": "2026-10-01T11:00:00Z", "endsAt": "2026-10-01T11:45:00Z", "durationMinutes": 45,
  "capacity": 20, "availableSlots": 18, "occupiedSlots": 2, "plan": "BASICO"
}
```
En POST/PUT se envían `name`, `description`, `instructor`, `roomId`, `startsAt`, `durationMinutes`,
`capacity` y `plan` (`BASICO`, `PLUS` o `PREMIUM`). Los cupos disponibles se calculan solos.

## Rutas internas de cupo (las llama ms-gymflow-reservations)

| Ruta | Cuándo |
|---|---|
| `POST /internal/services/{id}/take-slot` `{"reservationId": 42}` | Al CONFIRMAR una reserva |
| `POST /internal/services/{id}/release-slot` `{"reservationId": 42}` | Al CANCELAR una reserva confirmada |

Respuesta: `{"serviceId": 1, "reservationId": 42, "availableSlots": 17, "changed": true}`.
Errores: 404 clase inexistente, 409 sin cupos, 400 falta `reservationId`.

- **Sin sobreventa:** ambas rutas bloquean la fila de la clase (`SELECT ... FOR UPDATE`).
  `CupoConcurrenciaTest` lanza 10 confirmaciones simultáneas sobre 3 cupos y solo 3 lo consiguen.
- **Idempotentes:** la tabla `CUPO_RESERVADO` registra qué reserva ocupa cada cupo. Repetir `take-slot` o
  `release-slot` para la misma reserva no descuenta ni suma dos veces (`"changed": false`).

## Errores

Mismo JSON que el BFF: `{"timestamp", "status", "error", "message", "path"}`, con `message` en español.

## Ejecutar

```bash
# Local con H2 (modo PostgreSQL) y datos de ejemplo (sin RDS)
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# Con Amazon RDS PostgreSQL: completar DB_URL, DB_USERNAME y DB_PASSWORD en .env (ignorado por git)
cp .env.example .env
```

Si el puerto 8082 está ocupado en tu PC, agrega `--server.port=<otro>` (en Docker no importa).

## Pruebas

```bash
./mvnw test
```
Usan H2 en memoria en modo PostgreSQL; no necesitan RDS.
