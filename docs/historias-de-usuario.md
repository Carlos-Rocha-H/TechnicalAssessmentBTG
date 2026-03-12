# Épica: Gestión Self-Service de Fondos de Inversión — BTG Pactual

**Fecha:** 2026-03-11  
**Objetivo de negocio:** Permitir a los clientes gestionar sus fondos de inversión (suscripción, cancelación, consulta de historial) de forma autónoma, sin necesidad de contactar a un asesor, a través de una API REST.

**Stack tecnológico:** Java 25, Spring Boot 4.0.3, AWS DynamoDB, Maven

## Índice de Historias de Usuario

| # | ID | Título | Prioridad | Dependencias |
|---|-----|--------|-----------|--------------|
| 1 | HU-01 | Suscripción a fondo de inversión | Alta | HU-05, HU-06 |

| 2 | HU-02 | Cancelación de suscripción a fondo | Alta | HU-05, HU-06 |
| 3 | HU-03 | Consulta de historial de transacciones | Alta | HU-05, HU-06 |
| 4 | HU-04 | Notificación por email o SMS al suscribirse | Media | HU-01, HU-05 |
| 5 | HU-05 | Modelo de datos NoSQL (DynamoDB) | Alta | Ninguna |
| 6 | HU-06 | Autenticación, autorización y seguridad | Alta | Ninguna |
| 7 | HU-07 | Despliegue en AWS con CloudFormation | Media | HU-05, HU-06 |

**Secuencia de implementación sugerida:** HU-05 → HU-06 → HU-01 → HU-02 → HU-03 → HU-04 → HU-07

## Datos de Referencia — Fondos Disponibles

| ID | Nombre | Monto Mínimo | Categoría |
|----|--------|--------------|-----------|
| 1 | FPV_BTG_PACTUAL_RECAUDADORA | COP $75.000 | FPV |
| 2 | FPV_BTG_PACTUAL_ECOPETROL | COP $125.000 | FPV |
| 3 | DEUDAPRIVADA | COP $50.000 | FIC |
| 4 | FDO-ACCIONES | COP $250.000 | FIC |
| 5 | FPV_BTG_PACTUAL_DINAMICA | COP $100.000 | FPV |

**Saldo inicial del cliente:** COP $500.000

---

## HU-01: Suscripción a Fondo de Inversión

**ID:** HU-01  
**Prioridad:** Alta  
**Dependencias:** HU-05, HU-06

### Historia de Usuario

**Como** cliente de BTG Pactual,  
**Quiero** suscribirme a un fondo de inversión disponible,  
**Para** vincular mi capital al fondo seleccionado sin necesidad de contactar a un asesor.

### Endpoint

```
POST /api/v1/clients/{clientId}/subscriptions
```

**Request Body:**
```json
{
  "fundId": "1"
}
```

**Response (201 Created):**
```json
{
  "transactionId": "TXN-UUID-001",
  "clientId": "client-123",
  "fundId": "1",
  "fundName": "FPV_BTG_PACTUAL_RECAUDADORA",
  "type": "SUBSCRIPTION",
  "amount": 75000,
  "balance": 425000,
  "timestamp": "2026-03-11T10:30:00Z"
}
```

**Response (400 — saldo insuficiente):**
```json
{
  "error": "INSUFFICIENT_BALANCE",
  "message": "No tiene saldo disponible para vincularse al fondo FPV_BTG_PACTUAL_RECAUDADORA"
}
```

### Criterios de Aceptación

| # | Criterio |
|---|----------|
| CA-01 | Si el cliente tiene saldo >= monto mínimo del fondo, se crea la suscripción, se debita el monto y se retorna 201 con los datos de la transacción |
| CA-02 | Si el saldo es insuficiente, se retorna 400 con: "No tiene saldo disponible para vincularse al fondo {Nombre del fondo}" |
| CA-03 | La transacción se registra con un identificador único |
| CA-04 | Se dispara notificación (email o SMS) según preferencia del cliente al completar la suscripción |

### Reglas de Negocio

- Monto inicial del cliente: COP $500.000
- El monto debitado es el monto mínimo de vinculación del fondo
- Cada fondo tiene su monto mínimo según catálogo definido

---

## HU-02: Cancelar Suscripción a Fondo de Inversión

**ID:** HU-02  
**Prioridad:** Alta  
**Dependencias:** HU-05, HU-06

### Historia de Usuario

**Como** cliente de BTG Pactual,  
**Quiero** cancelar la suscripción a un fondo al que estoy vinculado,  
**Para** recuperar el valor de vinculación en mi saldo disponible.

### Endpoint

```
DELETE /api/v1/clients/{clientId}/subscriptions/{fundId}
```

**Response (200 OK):**
```json
{
  "transactionId": "TXN-UUID-002",
  "clientId": "client-123",
  "fundId": "1",
  "fundName": "FPV_BTG_PACTUAL_RECAUDADORA",
  "type": "CANCELLATION",
  "amount": 75000,
  "balance": 500000,
  "timestamp": "2026-03-11T11:00:00Z"
}
```

### Criterios de Aceptación

| # | Criterio |
|---|----------|
| CA-01 | Al cancelar la suscripción, el valor de vinculación se retorna al saldo del cliente |
| CA-02 | La transacción de cancelación se registra con un identificador único |
| CA-03 | El fondo cancelado ya no aparece como suscripción activa del cliente |

### Reglas de Negocio

- Al cancelar, se acredita al cliente el monto mínimo de vinculación del fondo
- La cancelación genera un registro de transacción tipo "CANCELLATION"

---

## HU-03: Ver Historial de Transacciones

**ID:** HU-03  
**Prioridad:** Alta  
**Dependencias:** HU-05, HU-06

### Historia de Usuario

**Como** cliente de BTG Pactual,  
**Quiero** ver el historial de mis transacciones (aperturas y cancelaciones),  
**Para** consultar los movimientos realizados sobre mis fondos de inversión.

### Endpoint

```
GET /api/v1/clients/{clientId}/transactions
```

**Response (200 OK):**
```json
[
  {
    "transactionId": "TXN-UUID-001",
    "fundId": "1",
    "fundName": "FPV_BTG_PACTUAL_RECAUDADORA",
    "type": "SUBSCRIPTION",
    "amount": 75000,
    "timestamp": "2026-03-11T10:30:00Z"
  },
  {
    "transactionId": "TXN-UUID-002",
    "fundId": "1",
    "fundName": "FPV_BTG_PACTUAL_RECAUDADORA",
    "type": "CANCELLATION",
    "amount": 75000,
    "timestamp": "2026-03-11T11:00:00Z"
  }
]
```

### Criterios de Aceptación

| # | Criterio |
|---|----------|
| CA-01 | Se retorna la lista de transacciones del cliente incluyendo aperturas y cancelaciones |
| CA-02 | Cada transacción muestra: id, fondo, tipo (SUBSCRIPTION/CANCELLATION), monto y fecha |

---

## HU-04: Notificación por Email o SMS al Suscribirse

**ID:** HU-04  
**Prioridad:** Media  
**Dependencias:** HU-01, HU-05

### Historia de Usuario

**Como** cliente de BTG Pactual,  
**Quiero** recibir una notificación por email o SMS al suscribirme a un fondo,  
**Para** tener confirmación del movimiento realizado según mi canal de preferencia.

### Descripción

Al completarse una suscripción exitosa (HU-01), el sistema envía una notificación al cliente por el canal configurado en su perfil (email o SMS). La preferencia de notificación es un dato del cliente.

### Criterios de Aceptación

| # | Criterio |
|---|----------|
| CA-01 | Al suscribirse exitosamente a un fondo, se envía notificación según la preferencia del cliente (email o SMS) |
| CA-02 | El cliente tiene configurada su preferencia de notificación (email o SMS) |
| CA-03 | La notificación incluye información del fondo al que se suscribió |

---

## HU-05: Modelo de Datos NoSQL (DynamoDB)

**ID:** HU-05  
**Prioridad:** Alta  
**Dependencias:** Ninguna (prerequisito técnico)

### Historia de Usuario

**Como** equipo de desarrollo,  
**Quiero** diseñar e implementar un modelo de datos NoSQL en DynamoDB que soporte las operaciones del sistema,  
**Para** persistir clientes, fondos, suscripciones y transacciones de forma eficiente.

### Modelo de Datos

**Tabla: Clients**

| Atributo | Tipo | Descripción |
|----------|------|-------------|
| clientId (PK) | String | Identificador único del cliente |
| name | String | Nombre del cliente |
| email | String | Email del cliente |
| phone | String | Teléfono del cliente |
| notificationPreference | String | "EMAIL" o "SMS" |
| balance | Number | Saldo disponible del cliente (inicial: 500000) |

**Tabla: Funds**

| Atributo | Tipo | Descripción |
|----------|------|-------------|
| fundId (PK) | String | Identificador del fondo |
| name | String | Nombre del fondo |
| minimumAmount | Number | Monto mínimo de vinculación |
| category | String | Categoría: "FPV" o "FIC" |

**Tabla: Transactions**

| Atributo | Tipo | Descripción |
|----------|------|-------------|
| transactionId (PK) | String | UUID único de la transacción |
| clientId (GSI) | String | ID del cliente (Global Secondary Index para consultas por cliente) |
| fundId | String | ID del fondo |
| fundName | String | Nombre del fondo |
| type | String | "SUBSCRIPTION" o "CANCELLATION" |
| amount | Number | Monto de la transacción |
| timestamp | String | Fecha/hora ISO 8601 |

**Tabla: Subscriptions**

| Atributo | Tipo | Descripción |
|----------|------|-------------|
| clientId (PK) | String | ID del cliente |
| fundId (SK) | String | ID del fondo |
| subscribedAt | String | Fecha de suscripción |
| amount | Number | Monto vinculado |

### Criterios de Aceptación

| # | Criterio |
|---|----------|
| CA-01 | Las tablas DynamoDB soportan las operaciones de suscripción, cancelación y consulta de historial |
| CA-02 | Las transacciones se pueden consultar por clientId mediante GSI |
| CA-03 | El catálogo de fondos contiene los 5 fondos especificados con sus montos mínimos |
| CA-04 | El modelo soporta la preferencia de notificación del cliente (email/SMS) |

---

## HU-06: Autenticación, Autorización y Seguridad

**ID:** HU-06  
**Prioridad:** Alta  
**Dependencias:** Ninguna (prerequisito técnico)

### Historia de Usuario

**Como** equipo de desarrollo,  
**Quiero** definir e implementar los procesos de autenticación, autorización, perfilamiento por roles y encriptación,  
**Para** asegurar que la API REST cumpla con buenas prácticas de seguridad.

### Definición Técnica

- **Autenticación:** JWT (JSON Web Token) emitido tras login exitoso. Cada request a la API debe incluir el token en el header `Authorization: Bearer <token>`.
- **Autorización:** Validación de que el cliente autenticado solo puede operar sobre sus propios recursos (`clientId` del token debe coincidir con el `clientId` del path).
- **Perfilamiento por roles:** Rol `CLIENT` para operaciones de suscripción, cancelación y consulta. Rol `ADMIN` para gestión de fondos y consultas globales.
- **Encriptación:** HTTPS/TLS para comunicación en tránsito. Datos sensibles (passwords) almacenados con hash BCrypt. DynamoDB encryption at rest habilitado.

### Criterios de Aceptación

| # | Criterio |
|---|----------|
| CA-01 | Los endpoints requieren autenticación vía JWT |
| CA-02 | Un cliente solo puede acceder a sus propios recursos (autorización por ownership) |
| CA-03 | Existen al menos dos roles: CLIENT y ADMIN |
| CA-04 | La comunicación se realiza sobre HTTPS y los datos sensibles están encriptados |

---

## HU-07: Despliegue en AWS con CloudFormation

**ID:** HU-07  
**Prioridad:** Media  
**Dependencias:** HU-05, HU-06

### Historia de Usuario

**Como** equipo de desarrollo,  
**Quiero** que el backend pueda desplegarse mediante AWS CloudFormation con documentación incluida,  
**Para** automatizar el aprovisionamiento de infraestructura y facilitar la replicabilidad del despliegue.

### Alcance

- Template CloudFormation (YAML) que provisione: tablas DynamoDB, función Lambda o ECS/Fargate para el API, API Gateway, roles IAM necesarios.
- Documentación de despliegue (README) con instrucciones paso a paso.
- Toda la infraestructura y recursos desplegados en AWS.

### Criterios de Aceptación

| # | Criterio |
|---|----------|
| CA-01 | Existe un template CloudFormation que despliega la infraestructura completa del backend |
| CA-02 | El template crea las tablas DynamoDB definidas en HU-05 |
| CA-03 | Se incluye documentación con instrucciones de despliegue |
| CA-04 | El stack de CloudFormation se puede crear y eliminar sin errores |
