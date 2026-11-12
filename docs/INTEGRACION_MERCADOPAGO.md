# Guía de integración: suscripciones con Mercado Pago

Guía para agregar membresías con cobro recurrente de Mercado Pago (MP) a un proyecto que ya funciona.
Está basada en el prototipo de este repositorio (`backend/` Spring Boot + `frontend/` React). Ese código sirve
como implementación de referencia.

**Leyenda**
- ✅ Confirmado por la documentación de MP o probado en el prototipo.
- ⚠️ Posible, pero la documentación no detalla el comportamiento: **verificar en sandbox antes de usarlo con socios reales**.
- ❌ No se puede (o no está soportado por la API).

---

## 1. Conceptos de Mercado Pago

| Recurso | Qué es | Endpoint base |
|---|---|---|
| **Plan** (`preapproval_plan`) | Plantilla: nombre, monto, frecuencia, prueba gratis. Tiene un link público (`init_point`) para que la gente se suscriba sola. | `/preapproval_plan` |
| **Suscripción** (`preapproval`) | El acuerdo de cobro recurrente con **un** pagador. Tiene **su propio** monto y frecuencia, con o sin plan asociado. | `/preapproval` |
| **Cuota / pago autorizado** (`authorized_payment`) | Cada cobro que genera una suscripción. | `/authorized_payments` |
| **Pago** (`payment`) | El pago concreto de una cuota. Las devoluciones se hacen sobre este recurso. | `/v1/payments` |

**Estados de una suscripción** (`status`): `pending` (esperando que el pagador autorice) → `authorized` (activa,
cobrando) ↔ `paused` → `cancelled` (definitivo).

**Idea clave:** el plan es el estándar, pero **cada suscripción tiene su propio monto**. Editar un plan no
debe asumirse como un cambio para los suscriptores existentes.

---

## 2. Credenciales y entorno

| Variable | Para qué | Notas |
|---|---|---|
| `MP_ACCESS_TOKEN` | Autenticación contra la API (`Authorization: Bearer ...`) | En pruebas: token del **usuario vendedor de prueba**. Nunca en el frontend ni en el repo. |
| `MP_WEBHOOK_SECRET` | Validar la firma `x-signature` de los webhooks | *Tus integraciones → Webhooks → Configurar notificaciones*. |
| `MP_BACK_URL` | A dónde vuelve el pagador después del checkout | ❌ MP no acepta `localhost`: tiene que ser https pública. |

**Sandbox**
- Crear **dos usuarios de prueba**: vendedor y comprador (mismo país). ⚠️ Las suscripciones fallan si se mezcla un usuario real con uno de prueba.
- El `payer_email` de la suscripción tiene que ser el email del **comprador de prueba**, y el pago se hace logueado con esa cuenta.
- Tarjetas de prueba: titular `APRO` para aprobar, `OTHE` para rechazar ([listado](https://www.mercadopago.com.ar/developers/es/docs/subscriptions/additional-content/your-integrations/test/cards)).
- Webhooks en local: túnel https (por ejemplo, ngrok) que exponga **solo** la ruta del webhook (ver §8).

---

## 3. Endpoints de Mercado Pago

Todos con:
```
Authorization: Bearer <MP_ACCESS_TOKEN>
Content-Type: application/json
X-Idempotency-Key: <uuid>        # recomendado en los POST, para no duplicar si se reintenta
```
Base: `https://api.mercadopago.com`

### 3.1 Planes

**Crear plan** ✅ `POST /preapproval_plan`
```json
{
  "reason": "Plan Plus",
  "auto_recurring": {
    "frequency": 1,
    "frequency_type": "months",
    "transaction_amount": 80000,
    "currency_id": "ARS",
    "repetitions": 12,
    "billing_day": 10,
    "billing_day_proportional": true,
    "free_trial": { "frequency": 1, "frequency_type": "months" }
  },
  "payment_methods_allowed": { "payment_types": [{}], "payment_methods": [{}] },
  "back_url": "https://tu-dominio.com/gracias"
}
```
Respuesta: `id` (es el `preapproval_plan_id`), `init_point` (link público), `status`.
Opcionales: `repetitions`, `billing_day`, `billing_day_proportional`, `free_trial`.

**Obtener plan** ✅ `GET /preapproval_plan/{id}`

**Buscar planes** ✅ `GET /preapproval_plan/search?status=active&limit=100&offset=0` → `{ paging, results: [...] }`

**Modificar plan** ✅ `PUT /preapproval_plan/{id}`: mismos campos que al crear (`reason`, `auto_recurring`, `back_url`).
- ⚠️ No está documentado si cambiar el monto del plan cambia el de los **suscriptores existentes**. Asumir que **no**: ajustar cada suscripción aparte (§3.2).

**Dar de baja un plan** ⚠️ `PUT /preapproval_plan/{id}` con `{ "status": "cancelled" }`
- ❌ No hay `DELETE`: los planes se dan de baja, no se borran.
- ⚠️ Verificar en sandbox el valor exacto del estado que acepta MP.

### 3.2 Suscripciones

Hay **dos formas** de crearlas:

| | A. Sin plan asociado (pendiente) | B. Con plan asociado |
|---|---|---|
| Body | `reason`, `payer_email`, `auto_recurring`, `back_url`, `external_reference`, `status: "pending"` | `preapproval_plan_id`, `payer_email`, `card_token_id`, `status: "authorized"` |
| Tarjeta | La carga el pagador en el checkout de MP (`init_point`) | **Requiere `card_token_id`**: hay que capturar la tarjeta en tu propio frontend con el SDK JS de MP (Card Payment Brick / CardForm) |
| Monto y frecuencia propios | ✅ Sí, por suscripción | Hereda del plan |
| `external_reference` (tu ID) | ✅ | ✅ |
| Aparece agrupada bajo el plan en MP | ❌ | ✅ |
| Usado en el prototipo | ✅ (permite promos y precios por persona) | — |

Hay una tercera vía: el **link público del plan** (`init_point` del plan). El socio se suscribe solo y queda asociado al
plan en MP, pero sin `external_reference`. Tu sistema se entera por webhook y lo identifica por `payer_email` +
`preapproval_plan_id`.

**Crear (A, pendiente)** ✅ `POST /preapproval`
```json
{
  "reason": "Plan Plus",
  "external_reference": "SUB-42",
  "payer_email": "comprador@test.com",
  "auto_recurring": {
    "frequency": 1,
    "frequency_type": "months",
    "transaction_amount": 40000,
    "currency_id": "ARS"
  },
  "back_url": "https://tu-dominio.com/gracias",
  "status": "pending"
}
```
Respuesta: `id`, `init_point` (link de pago para mandarle al socio), `status: "pending"`.

**Crear (B, con plan)** ✅ `POST /preapproval`
```json
{
  "preapproval_plan_id": "2c938084...",
  "payer_email": "comprador@test.com",
  "card_token_id": "e3ed6f09...",
  "external_reference": "SUB-42",
  "back_url": "https://tu-dominio.com/gracias",
  "status": "authorized"
}
```

**Obtener** ✅ `GET /preapproval/{id}`. Campos útiles: `status`, `external_reference`, `payer_email`,
`preapproval_plan_id`, `auto_recurring`, `next_payment_date`, `summarized` (cuotas cobradas y pendientes).
⚠️ Confirmar en sandbox los nombres exactos de `next_payment_date` y `summarized` antes de depender de ellos.

**Buscar** ✅ `GET /preapproval/search?...` (filtros por estado, pagador, plan, etc.; ver referencia de la API).

**Modificar** `PUT /preapproval/{id}`:

| Cambio | Body | Estado |
|---|---|---|
| Pausar | `{ "status": "paused" }` | ✅ |
| Reactivar | `{ "status": "authorized" }` | ✅ (⚠️ efecto sobre los cobros: ver §5) |
| Cancelar (definitivo) | `{ "status": "cancelled" }` | ✅ |
| Cambiar monto | `{ "auto_recurring": { "transaction_amount": 40000, "currency_id": "ARS" } }` | ✅ |
| Cambiar tarjeta | `{ "card_token_id": "..." }` | ✅ |
| Medio de pago secundario | `{ "card_token_id_secondary": "...", "payment_method_id_secondary": "..." }` | ✅ |
| Cambiar día de cobro (mensual) | campo de día de cobro | ✅ documentado (⚠️ confirmar nombre del campo) |
| Cambiar frecuencia | — | ❌ no documentado (ver §4) |

### 3.3 Cuotas y pagos

| Acción | Endpoint | Estado |
|---|---|---|
| Ver una cuota | `GET /authorized_payments/{id}` (incluye el `payment` asociado y su estado) | ⚠️ verificar campos |
| Devolver un pago (total) | `POST /v1/payments/{payment_id}/refunds` (body vacío) | ✅ |
| Devolver parcial | `POST /v1/payments/{payment_id}/refunds` con `{ "amount": 1000 }` | ✅ |

---

## 4. Qué se puede y qué no

### Precio
| Necesidad | ¿Se puede? | Cómo |
|---|---|---|
| Precio distinto por persona con el mismo plan | ✅ | Suscripción tipo A con su monto, o `PUT` del monto después. |
| Cambiar el precio de un socio ya suscripto | ✅ | `PUT /preapproval/{id}` → `auto_recurring.transaction_amount`. Rige desde el próximo cobro. MP le avisa al socio. |
| Promo de lanzamiento (ej. $80.000 → $40.000 si se suscribe antes del 31/12) | ✅ | Calcular el monto con descuento al crear. Si es temporal, un job vuelve al precio de lista con `PUT`. |
| Cobrar $0 en una cuota | ❌ | El monto tiene que ser mayor a 0 (y MP tiene mínimos por moneda). |
| Primer período gratis | ✅ | `free_trial` (plan o suscripción). Solo al **inicio**. |
| "3er mes gratis" | ⚠️ | No hay soporte nativo. Alternativas en §5. |
| Que editar el plan actualice a todos sus socios | ⚠️ | No documentado. Si hace falta, recorrer las suscripciones y hacer `PUT` en cada una. |

### Tiempo y frecuencia
| Necesidad | ¿Se puede? | Cómo |
|---|---|---|
| Mensual para unos y trimestral para otros, con el "mismo plan" | ✅ al crear | Tipo A: cada suscripción con su `frequency`/`frequency_type` (`frequency: 3, frequency_type: "months"`). Por link de plan: un plan en MP por frecuencia, agrupados en tu sistema. |
| Cambiar de mensual a trimestral a alguien ya suscripto | ❌ | No documentado: cancelar y crear una suscripción nueva (el socio vuelve a autorizar). |
| Fijar día de cobro mensual | ✅ | `billing_day` en el plan; cambio documentado para suscripciones mensuales. |
| Cobrar proporcional el primer mes | ✅ | `billing_day_proportional: true`. |
| Cantidad fija de cobros o fecha de fin | ✅ | `repetitions` (plan) / `end_date` (suscripción). |
| Permanencia mínima (no dejar cancelar antes de N meses) | ❌ en MP | **Regla propia de tu sistema**: el socio puede cancelar desde su cuenta de MP. Ver §6. |

### Gestión
| Necesidad | ¿Se puede? | Notas |
|---|---|---|
| Pausar y reactivar | ✅ | ⚠️ No está documentado qué pasa con los cobros durante la pausa ni al reactivar. |
| Cancelar | ✅ | Definitivo: para volver hay que crear una suscripción nueva. |
| Borrar un plan | ❌ | Solo darlo de baja. |
| Que el socio gestione su suscripción | ✅ | Desde su cuenta de MP (pausar/cancelar). **Tu sistema tiene que enterarse por webhook.** |
| Saber de cada cobro (ok / rechazado) | ✅ | Webhook `subscription_authorized_payment`. |
| Webhooks a `localhost` | ❌ | URL https pública. |
| Asociar a un plan sin capturar tarjeta propia | ❌ | Tipo B requiere `card_token_id`. Usar tipo A o el link del plan. |

---

## 5. "Un mes gratis en el medio" (ej. el 3er mes)

| Opción | Cómo | Riesgo |
|---|---|---|
| **Pausa durante el mes N** | Job: `paused` un día antes del cobro N y `authorized` un día después. Localmente el socio sigue activo, con la marca "mes bonificado". | ⚠️ **Sin documentar**: al reactivar MP podría cobrar el mes omitido o correr las fechas siguientes. Si falla la reactivación, el socio queda pausado y se deja de cobrar. |
| **Devolver el cobro N** | Webhook `subscription_authorized_payment` → si es la cuota N → `POST /v1/payments/{id}/refunds`. | El socio ve un cobro y su devolución. Comportamiento documentado y predecible. |
| **Primer mes gratis** | `free_trial`. | Nativo, pero el mes gratis es el 1°. |
| **Repartir el descuento** | "3 meses al precio de 2": 33,33% off los primeros 3 meses. | Ninguno técnico: es una promo con vencimiento. |

**Prueba obligatoria antes de usar la pausa:** crear en sandbox una suscripción con **frecuencia diaria**
(`frequency_type: "days"`), pausarla antes de un cobro, reactivarla después y observar tres cosas:
(1) si saltea el cobro, (2) si lo cobra al reactivar, (3) si se corre la fecha del siguiente cobro.
Si pasa (2) o (3), usar la opción de devolución.

---

## 6. Reglas de negocio que viven en tu sistema (no en MP)

MP solo cobra. Esto lo tiene que modelar tu backend:

| Regla | Implementación de referencia |
|---|---|
| **Permanencia mínima** (3 meses) | Si piden la baja antes de `fecha_alta + N meses`, queda **programada** para esa fecha y un job diario la ejecuta (`PUT status=cancelled`). ⚠️ Si el socio cancela desde su cuenta de MP, llega por webhook y no se puede impedir: resolverlo comercial o contractualmente (por ejemplo, cargo por baja anticipada). |
| **Niveles de membresía** (3) | Campo `tier` en el plan, un plan activo por nivel. Upgrade/downgrade = `PUT` del monto. No reinicia la permanencia. |
| **Beneficios y horas** | Lista de beneficios y horas por mes en el plan. Los consumos descuentan horas del período, que se renueva en la fecha aniversario del alta. |
| **Promociones** | Ventana de fechas de alta, % de descuento y duración en meses. Job diario que vuelve al precio de lista. |
| **Socios activos** | Suscripciones en `authorized` según el último estado conocido (webhook + sincronización). |

Código de referencia: `backend/src/main/java/com/testsusmp/subs/domain/` (`PermanencePolicy`, `PricingCalculator`,
`HoursCalculator`, `TierPolicy`). Son funciones puras con tests.

---

## 7. Modelo de datos sugerido

Agregar a la base existente (SQL compatible con PostgreSQL; ver `backend/src/main/resources/db/migration/V1__init.sql`):

```sql
plans          (id, name, description, tier, price, currency, monthly_hours, min_permanence_months,
                active, gateway_plan_id, gateway_init_point, created_at, updated_at)
plan_benefits  (plan_id, sort_order, benefit)
promotions     (id, name, valid_from, valid_to, discount_percent, duration_months, active)
members        (id, full_name, email UNIQUE, created_at)            -- o reutilizar tu tabla de usuarios
subscriptions  (id, member_id, plan_id, status, amount, promotion_id, promo_active, promo_ends_on,
                start_date, min_end_date, scheduled_cancel_date, cancelled_at,
                gateway_id UNIQUE, init_point, created_at)
consumptions   (id, subscription_id, consumed_on, hours, description, created_at)
```

- `subscriptions.gateway_id` = `preapproval.id` de MP; `external_reference` = `SUB-{subscriptions.id}`.
- Si el proyecto ya tiene usuarios, `members` puede ser una FK a esa tabla. El email tiene que coincidir con la cuenta de MP del pagador.

---

## 8. Webhooks

**Configuración:** *Tus integraciones → tu app → Webhooks*, URL `https://tu-dominio/api/webhooks/mercadopago`,
evento **Planes y suscripciones**.

| Tópico (`type`) | Cuándo llega | Qué hacer |
|---|---|---|
| `subscription_preapproval` | Alta o cambio de una suscripción (incluye pausas y cancelaciones hechas por el socio) | `GET /preapproval/{data.id}` y aplicar el estado. Si no existe localmente y trae `preapproval_plan_id`, es un alta desde el link del plan: crear el socio. |
| `subscription_preapproval_plan` | Alta o cambio de un plan (por ejemplo, desde el panel de MP) | Sincronizar planes (`GET /preapproval_plan/search`). |
| `subscription_authorized_payment` | Cada cuota cobrada o rechazada | Registrar el pago; avisar si es rechazado; devolver la cuota si es un "mes bonificado" (§5). |

**Validar la firma** ✅ (rechazar con 401 si no coincide):
```
header x-signature: ts=1704908010,v1=<hash>
manifest  = "id:" + data.id (en minúsculas si es alfanumérico) + ";request-id:" + x-request-id + ";ts:" + ts + ";"
esperado  = HMAC_SHA256(clave_secreta, manifest) en hex
válido    = comparación en tiempo constante entre esperado y v1
```
`data.id` llega como query param (`?data.id=...`) y también en el body (`data.id`).
Implementación de referencia: `WebhookSignatureValidator.java` (con tests).

**Reglas**
- Responder **200/201 en menos de 22 s**. Si no, MP reintenta cada 15 minutos. Procesar rápido o encolar.
- **Idempotencia:** el mismo aviso puede llegar varias veces. Consultar siempre el estado actual con `GET`; nunca acumular a partir del body.
- **No confiar solo en el webhook:** tener una **sincronización periódica** (`GET` de cada suscripción vigente) como respaldo.
- **Seguridad:** si la API del tablero no tiene login, exponer a internet **solo** la ruta del webhook
  (ver `scripts/ngrok-webhook-policy.yml`).

---

## 9. Endpoints del módulo (contrato de referencia)

Lo que el prototipo expone para el tablero. Es un buen contrato para replicar en el proyecto existente.

| Método | Ruta | Qué hace | Llamada a MP |
|---|---|---|---|
| GET | `/api/dashboard/summary` | KPIs, socios por nivel, promo vigente, modo | — |
| GET | `/api/plans?activeOnly=` | Listar planes | — |
| POST | `/api/plans` | Crear plan | `POST /preapproval_plan` |
| GET | `/api/plans/{id}` | Ver plan | — |
| PUT | `/api/plans/{id}` | Modificar plan | `PUT /preapproval_plan/{id}` |
| DELETE | `/api/plans/{id}` | Baja lógica | `PUT /preapproval_plan/{id}` (cancelled) |
| POST | `/api/plans/{id}/activate` | Reactivar | `POST /preapproval_plan` (nuevo) |
| POST | `/api/plans/sync` | Traer planes de MP | `GET /preapproval_plan/search` |
| GET | `/api/subscriptions?status=` | Listar suscripciones | — |
| POST | `/api/subscriptions` | Alta → devuelve link de pago | `POST /preapproval` (pending) |
| GET | `/api/subscriptions/{id}` | Ver suscripción | — |
| POST | `/api/subscriptions/{id}/pause` | Pausar | `PUT /preapproval/{id}` (paused) |
| POST | `/api/subscriptions/{id}/resume` | Reanudar | `PUT /preapproval/{id}` (authorized) |
| POST | `/api/subscriptions/{id}/cancel` | Baja inmediata o programada (permanencia) | `PUT /preapproval/{id}` (cancelled) si es inmediata |
| DELETE | `/api/subscriptions/{id}/cancel` | Deshacer baja programada | — |
| PUT | `/api/subscriptions/{id}/plan` | Upgrade / downgrade | `PUT /preapproval/{id}` (monto) |
| GET / POST | `/api/subscriptions/{id}/consumptions` | Horas del período / registrar consumo | — |
| POST | `/api/subscriptions/sync` | Traer estados de MP | `GET /preapproval/{id}` × N |
| GET | `/api/members/active` | Socios activos | — |
| GET / POST / PUT | `/api/promotions` | Promociones | — |
| POST | `/api/admin/jobs/run` | Ejecutar bajas programadas y fin de promos | `PUT /preapproval/{id}` |
| POST | `/api/webhooks/mercadopago` | Notificaciones de MP | `GET /preapproval/{id}` / `GET /preapproval_plan/search` |

Errores: `404` inexistente, `409` regla de negocio violada (permanencia, nivel ocupado, horas insuficientes),
`502` error de MP (con el mensaje de MP), `400` validación.

---

## 10. Pasos para implementarlo en el proyecto existente

1. **Credenciales y configuración**: variables de entorno (§2), nunca en el repo. Usuarios de prueba creados.
2. **Puerto de pasarela**: una interfaz (`PaymentGateway`) con dos implementaciones: MP real y una simulada para
   tests y desarrollo. Así el resto del código no depende de MP.
3. **Migraciones** de las tablas de §7, o adaptarlas a las entidades existentes (usuarios → socios).
4. **Reglas de negocio** de §6 como funciones puras, con tests primero.
5. **Planes**: ABM local + reflejo en MP + sincronización desde MP.
6. **Suscripciones**: alta tipo A con `external_reference`, gestión de estados, cambio de monto.
7. **Webhook** con validación de firma, idempotente y rápido. Exponer solo esa ruta.
8. **Jobs**: diario (bajas programadas, fin de promos) y sincronización periódica (cada 5–15 min).
9. **Tablero**: planes, suscripciones, socios activos, consumos y promociones.
10. **Sandbox end-to-end**: crear plan → suscripción → pagar con el comprador de prueba → recibir el webhook →
    pausar/cancelar desde la cuenta del comprador → verificar que el sistema se entere.
11. **Producción**: credenciales de producción, URL del webhook de producción, `back_url` real, login en el tablero, monitoreo de errores de MP.

---

## 11. Checklist de verificación en sandbox (puntos ⚠️)

- [ ] Baja de plan: valor de `status` que acepta `PUT /preapproval_plan/{id}`.
- [ ] Editar el monto de un plan: ¿cambia el de los suscriptores existentes?
- [ ] Pausa: ¿se saltea el cobro? ¿se cobra al reactivar? ¿se corren las fechas? (frecuencia diaria)
- [ ] Nombres de campos en `GET /preapproval/{id}`: `next_payment_date`, `summarized`.
- [ ] Campos de `GET /authorized_payments/{id}` (cuota N, `payment.id` para devolver).
- [ ] Alta desde el link del plan: llega `subscription_preapproval` con `preapproval_plan_id` y `payer_email`.
- [ ] Cancelación desde la cuenta del comprador: llega el webhook y el sistema la refleja.
- [ ] Monto mínimo aceptado por MP en ARS.

## 12. Decisiones abiertas (negocio)

- Promo "$80.000 → $40.000 antes del 31/12": ¿por cuántos meses o congelado para siempre? ¿todos los planes o uno?
- Precio personalizado por socio: ¿hace falta en el tablero? ¿con vencimiento?
- Frecuencia trimestral: precio (¿3× o con descuento?) y reparto de horas.
- Mes bonificado: ¿pausa o devolución? (depende de la verificación de §5). ¿Cambia la permanencia mínima?

## 13. Referencias

- Planes de suscripción: https://www.mercadopago.com.ar/developers/es/docs/subscription-plans/overview
- Suscripciones: https://www.mercadopago.com.ar/developers/es/docs/subscriptions/overview
- Con plan asociado: https://www.mercadopago.com.ar/developers/es/docs/subscriptions/integration-configuration/subscription-associated-plan
- Sin plan asociado: https://www.mercadopago.com.ar/developers/es/docs/subscriptions/integration-configuration/subscription-no-associated-plan/introduction
- Gestión: https://www.mercadopago.com.ar/developers/es/docs/subscriptions/subscription-management
- Webhooks: https://www.mercadopago.com.ar/developers/es/docs/subscriptions/additional-content/your-integrations/notifications/webhooks
- Cancelaciones y devoluciones: https://www.mercadopago.com.ar/developers/es/docs/subscriptions/additional-content/cancellations-and-refunds
