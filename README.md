# BTG Pactual — Investment Fund Self-Service API

API REST de autogestión de fondos de inversión para clientes BTG Pactual. Permite registrar usuarios, suscribirse a fondos, cancelar suscripciones, consultar historial de transacciones y recibir notificaciones por email/SMS.

---

## Tabla de Contenido

- [Stack Tecnológico](#stack-tecnológico)
- [Arquitectura del Microservicio](#arquitectura-del-microservicio)
- [Estructura del Proyecto](#estructura-del-proyecto)
- [Modelo de Datos (DynamoDB)](#modelo-de-datos-dynamodb)
- [Endpoints API](#endpoints-api)
- [DTOs (Data Transfer Objects)](#dtos-data-transfer-objects)
- [Capa de Servicios](#capa-de-servicios)
- [Seguridad (JWT + Ownership)](#seguridad-jwt--ownership)
- [Manejo de Excepciones](#manejo-de-excepciones)
- [Notificaciones (SES / SNS)](#notificaciones-ses--sns)
- [Desarrollo Local](#desarrollo-local)
- [Despliegue en AWS con CloudFormation](#despliegue-en-aws-con-cloudformation)
- [Recursos Creados por CloudFormation](#recursos-creados-por-cloudformation)
- [Tests](#tests)
- [Punto 2 — Query SQL (Álgebra Relacional)](#punto-2--query-sql-álgebra-relacional)

---

## Stack Tecnológico

| Tecnología | Uso |
|---|---|
| **Java 25** + **Spring Boot 4.0.3** | Framework base del microservicio |
| **AWS DynamoDB** (Enhanced Client v2.31.1) | Persistencia NoSQL |
| **AWS SES** | Notificaciones por email |
| **AWS SNS** | Notificaciones por SMS |
| **Spring Security + JWT** (jjwt 0.12.6) | Autenticación y autorización |
| **SpringDoc OpenAPI** (2.8.6) | Documentación Swagger UI |
| **Lombok** | Reducción de boilerplate |
| **ECS Fargate** | Contenedorización serverless |
| **API Gateway** | Punto de entrada HTTP |
| **CloudFormation** | Infraestructura como código |
| **LocalStack** (Docker Compose) | Emulación local de servicios AWS |

---

## Arquitectura del Microservicio

```
┌─────────────┐     ┌──────────────────┐     ┌──────────────────┐     ┌─────────────────┐
│   Cliente    │────▶│  API Gateway /   │────▶│   Controllers    │────▶│    Services      │
│  (HTTP/JWT)  │     │  Spring Security │     │  Auth, Sub, Txn  │     │  Business Logic  │
└─────────────┘     └──────────────────┘     └──────────────────┘     └────────┬────────┘
                                                                               │
                                              ┌────────────────────────────────┼────────────────┐
                                              ▼                                ▼                 ▼
                                     ┌─────────────────┐            ┌──────────────┐   ┌──────────────┐
                                     │   Repositories   │            │  AWS SES     │   │  AWS SNS     │
                                     │   (DynamoDB)     │            │  (Email)     │   │  (SMS)       │
                                     └─────────────────┘            └──────────────┘   └──────────────┘
```

**Flujo principal:**
1. El cliente se autentica vía `/api/v1/auth/login` o se registra vía `/api/v1/auth/register` y obtiene un JWT.
2. Con el JWT, puede suscribirse a fondos, cancelar suscripciones y consultar transacciones.
3. Cada operación de suscripción/cancelación genera una transacción y dispara una notificación (email o SMS) según la preferencia del cliente.
4. Un `OwnershipValidator` garantiza que un cliente solo acceda a sus propios recursos (ADMIN puede acceder a cualquiera).

---

## Estructura del Proyecto

```
btgpactual/
├── infrastructure/
│   └── cloudformation.yaml                    # Template CloudFormation (ECS, DynamoDB, API GW, IAM, etc.)
├── docs/
│   └── historias-de-usuario.md                # Historias de usuario del producto
├── 2do punto/
│   ├── Creación de tablas.sql                 # DDL para modelo relacional (SQL Server)
│   └── Query.sql                              # Consulta SQL de álgebra relacional
├── docker-compose.yml                         # LocalStack para desarrollo local
├── Dockerfile                                 # Imagen Docker del microservicio
├── pom.xml                                    # Dependencias Maven
├── src/main/java/.../btgpactual/
│   ├── BtgpactualApplication.java             # Clase principal Spring Boot
│   ├── config/
│   │   ├── AwsConfig.java                     # Beans AWS (SES, SNS clients)
│   │   ├── DynamoDbConfig.java                # DynamoDB Enhanced Client + tabla registrations
│   │   ├── DataSeeder.java                    # Creación de tablas y datos semilla al arrancar
│   │   └── OpenApiConfig.java                 # Configuración Swagger/OpenAPI con JWT
│   ├── controller/
│   │   ├── AuthController.java                # POST /auth/login, POST /auth/register
│   │   ├── SubscriptionController.java        # POST/DELETE subscriptions (con ownership check)
│   │   └── TransactionController.java         # GET transactions (con ownership check)
│   ├── dto/
│   │   ├── LoginRequest.java                  # username, password
│   │   ├── LoginResponse.java                 # token, userId, clientId, role
│   │   ├── RegisterRequest.java               # username, password, name, email, phone, notificationPreference
│   │   ├── SubscriptionRequest.java           # fundId, amount
│   │   ├── SubscriptionResponse.java          # Respuesta completa de suscripción/cancelación
│   │   ├── TransactionResponse.java           # Respuesta de historial de transacciones
│   │   └── ErrorResponse.java                 # error, message (formato estándar de errores)
│   ├── model/
│   │   ├── Client.java                        # DynamoDB: PK=clientId
│   │   ├── Fund.java                          # DynamoDB: PK=fundId
│   │   ├── Subscription.java                  # DynamoDB: PK=clientId, SK=fundId
│   │   ├── Transaction.java                   # DynamoDB: PK=transactionId, GSIs: clientId, subscriptionId
│   │   └── User.java                          # DynamoDB: PK=userId, GSI: username
│   ├── repository/
│   │   ├── ClientRepository.java              # CRUD clientes
│   │   ├── FundRepository.java                # CRUD fondos
│   │   ├── SubscriptionRepository.java        # CRUD suscripciones (clave compuesta)
│   │   ├── TransactionRepository.java         # CRUD transacciones + queries por GSIs
│   │   └── UserRepository.java                # CRUD usuarios + búsqueda por username
│   ├── service/
│   │   ├── AuthService.java                   # Login (JWT), registro de usuario+cliente
│   │   ├── SubscriptionService.java           # Suscripción y cancelación de fondos
│   │   ├── TransactionService.java            # Consulta de historial de transacciones
│   │   └── notification/
│   │       ├── NotificationService.java       # Orquestador de notificaciones
│   │       ├── NotificationSender.java        # Interface: send(destination, subject, body) + getType()
│   │       ├── EmailNotificationSender.java   # Implementación via AWS SES
│   │       └── SmsNotificationSender.java     # Implementación via AWS SNS
│   ├── security/
│   │   ├── SecurityConfig.java                # Filtros, rutas públicas/privadas, CORS, stateless
│   │   ├── JwtUtil.java                       # Generación y validación de JWT
│   │   ├── JwtAuthenticationFilter.java       # Filtro que extrae JWT del header Authorization
│   │   ├── JwtAuthenticationEntryPoint.java   # Respuesta 401 personalizada
│   │   ├── JwtAccessDeniedHandler.java        # Respuesta 403 personalizada
│   │   ├── JwtUserDetails.java                # Detalles del usuario autenticado (userId, clientId, role)
│   │   ├── OwnershipValidator.java            # Valida que CLIENT acceda solo a sus recursos
│   │   └── AccessForbiddenException.java      # Excepción de acceso denegado por ownership
│   └── exception/
│       ├── GlobalExceptionHandler.java        # @ControllerAdvice con mapeo HTTP centralizado
│       ├── InsufficientBalanceException.java  # Saldo insuficiente → 400
│       ├── BelowMinimumAmountException.java   # Monto bajo mínimo del fondo → 400
│       ├── DuplicateSubscriptionException.java# Suscripción duplicada → 400
│       ├── ClientNotFoundException.java       # Cliente no encontrado → 404
│       ├── FundNotFoundException.java         # Fondo no encontrado → 404
│       └── SubscriptionNotFoundException.java # Suscripción no encontrada → 404
└── src/test/java/.../btgpactual/              # 73 tests unitarios
    ├── BtgpactualApplicationTests.java
    ├── controller/                            # Tests de controladores (17 tests)
    ├── model/                                 # Tests de modelos (9 tests)
    ├── security/                              # Tests de JWT y ownership (10 tests)
    └── service/                               # Tests de servicios (36 tests)
```

---

## Modelo de Datos (DynamoDB)

### Tablas y Claves

| Tabla | Partition Key | Sort Key | GSIs |
|-------|---------------|----------|------|
| **Clients** | `clientId` (String) | — | — |
| **Funds** | `fundId` (String) | — | — |
| **Subscriptions** | `clientId` (String) | `fundId` (String) | — |
| **Transactions** | `transactionId` (String) | — | `clientId-index`, `subscriptionId-index` |
| **Users** | `userId` (String) | — | `username-index` |

### Entidades

**Client** — Representa un cliente del sistema.
- `clientId`, `name`, `email`, `phone`, `balance`, `notificationPreference` (EMAIL/SMS)

**Fund** — Fondo de inversión disponible.
- `fundId`, `name`, `minimumAmount`, `category`

**Subscription** — Suscripción activa de un cliente a un fondo (clave compuesta).
- `clientId`, `fundId`, `amount`, `timestamp`

**Transaction** — Registro inmutable de cada operación.
- `transactionId`, `subscriptionId`, `clientId`, `fundId`, `fundName`, `type` (apertura/cancelación), `amount`, `timestamp`

**User** — Credenciales para autenticación.
- `userId`, `username`, `passwordHash`, `role` (CLIENT/ADMIN), `clientId`

### Datos Semilla (DataSeeder)

Al arrancar, la aplicación crea las tablas si no existen y carga:
- **5 fondos**: FPV_BTG_PACTUAL_RECAUDADORA, FPV_BTG_PACTUAL_ECOPETROL, DEUDAPRIVADA, FDO_BTG_PACTUAL_DINAMICA, FPV_BTG_PACTUAL_ACCIONES
- **1 cliente demo**: con saldo inicial de $500,000 COP
- **2 usuarios**: `cliente1` (rol CLIENT) y `admin` (rol ADMIN)

---

## Endpoints API

| Método | Endpoint | Descripción | Auth | Roles |
|--------|----------|-------------|------|-------|
| `POST` | `/api/v1/auth/login` | Autenticar usuario y obtener JWT | No | — |
| `POST` | `/api/v1/auth/register` | Registrar nuevo usuario + cliente | No | — |
| `POST` | `/api/v1/clients/{clientId}/subscriptions` | Suscribirse a un fondo | JWT | CLIENT, ADMIN |
| `DELETE` | `/api/v1/clients/{clientId}/subscriptions/{fundId}` | Cancelar suscripción activa | JWT | CLIENT, ADMIN |
| `GET` | `/api/v1/clients/{clientId}/transactions` | Consultar historial de transacciones | JWT | CLIENT, ADMIN |

### Ejemplos de Uso

**Login:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"cliente1","password":"password123"}'
```

**Registro:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "nuevo_usuario",
    "password": "miPassword123",
    "name": "Juan Pérez",
    "email": "juan@example.com",
    "phone": "+573001234567",
    "notificationPreference": "EMAIL"
  }'
```

**Suscripción a fondo:**
```bash
curl -X POST http://localhost:8080/api/v1/clients/{clientId}/subscriptions \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"fundId":"fund-001","amount":75000}'
```

**Cancelar suscripción:**
```bash
curl -X DELETE http://localhost:8080/api/v1/clients/{clientId}/subscriptions/{fundId} \
  -H "Authorization: Bearer <TOKEN>"
```

**Consultar transacciones:**
```bash
curl http://localhost:8080/api/v1/clients/{clientId}/transactions \
  -H "Authorization: Bearer <TOKEN>"
```

---

## DTOs (Data Transfer Objects)

| DTO | Campos | Uso |
|-----|--------|-----|
| `LoginRequest` | `username`, `password` | Request de login |
| `LoginResponse` | `token`, `userId`, `clientId`, `role` | Response de login/registro |
| `RegisterRequest` | `username`, `password`, `name`, `email`, `phone`, `notificationPreference` | Request de registro |
| `SubscriptionRequest` | `fundId`, `amount` | Request de suscripción |
| `SubscriptionResponse` | `transactionId`, `subscriptionId`, `clientId`, `fundId`, `fundName`, `type`, `amount`, `balance`, `timestamp` | Response de suscripción/cancelación |
| `TransactionResponse` | `transactionId`, `fundId`, `fundName`, `type`, `amount`, `timestamp` | Response de historial |
| `ErrorResponse` | `error`, `message` | Response de errores |

---

## Capa de Servicios

### AuthService
- **`login(LoginRequest)`** — Valida credenciales, genera JWT con claims `userId`, `clientId` y `role`.
- **`register(RegisterRequest)`** — Valida datos, previene usuarios duplicados, crea `Client` con saldo inicial + `User` con password hasheado, retorna JWT.

### SubscriptionService
- **`subscribe(clientId, SubscriptionRequest)`** — Verifica existencia de cliente/fondo, valida monto mínimo, verifica que no haya suscripción duplicada, debita saldo, crea suscripción, registra transacción de apertura, envía notificación.
- **`cancel(clientId, fundId)`** — Verifica existencia de cliente/fondo/suscripción, acredita saldo, elimina suscripción, registra transacción de cancelación.

### TransactionService
- **`getTransactionsByClientId(clientId)`** — Verifica que el cliente exista, consulta transacciones por GSI `clientId-index`, mapea a `TransactionResponse`.

### NotificationService
- **`notifySubscription(Client, Fund, String type)`** — Selecciona el canal (EMAIL/SMS) según preferencia del cliente, construye mensaje y delega al `NotificationSender` correspondiente.
  - `EmailNotificationSender` → AWS SES
  - `SmsNotificationSender` → AWS SNS

---

## Seguridad (JWT + Ownership)

### Autenticación JWT
- Token firmado con clave secreta (`jwt.secret`), expiración configurable (`jwt.expiration-ms`).
- Claims: `sub` = userId, `clientId`, `role`.
- Filtro `JwtAuthenticationFilter` extrae el token del header `Authorization: Bearer <token>` y popula el `SecurityContext`.

### Autorización
- **Rutas públicas:** `/api/v1/auth/**`, Swagger (`/swagger-ui/**`, `/v3/api-docs/**`).
- **Rutas protegidas:** Todos los endpoints de clientes requieren `ROLE_CLIENT` o `ROLE_ADMIN`.
- **Sesiones:** Stateless (sin sesión de servidor).

### Validación de Ownership
- `OwnershipValidator` compara el `clientId` del JWT con el `{clientId}` del path.
- **ADMIN** puede acceder a recursos de cualquier cliente.
- **CLIENT** solo puede acceder a sus propios recursos.
- Lanza `AccessForbiddenException` (HTTP 403) si no coincide.

### Respuestas de Error de Seguridad
- `JwtAuthenticationEntryPoint` → HTTP 401 con `ErrorResponse`.
- `JwtAccessDeniedHandler` → HTTP 403 con `ErrorResponse`.

**Usuarios precargados:**

| Usuario | Password | Rol | ClientId |
|---------|----------|-----|----------|
| `cliente1` | `password123` | CLIENT | `550e8400-e29b-41d4-a716-446655440000` |
| `admin` | `admin123` | ADMIN | — |

---

## Manejo de Excepciones

El `GlobalExceptionHandler` (`@ControllerAdvice`) centraliza todas las respuestas de error:

| Excepción | HTTP Status | Código Error |
|-----------|-------------|--------------|
| `AccessForbiddenException` | 403 Forbidden | `ACCESS_FORBIDDEN` |
| `IllegalArgumentException` | 401 Unauthorized | `UNAUTHORIZED` |
| `InsufficientBalanceException` | 400 Bad Request | `INSUFFICIENT_BALANCE` |
| `BelowMinimumAmountException` | 400 Bad Request | `BELOW_MINIMUM_AMOUNT` |
| `DuplicateSubscriptionException` | 400 Bad Request | `DUPLICATE_SUBSCRIPTION` |
| `ClientNotFoundException` | 404 Not Found | `CLIENT_NOT_FOUND` |
| `FundNotFoundException` | 404 Not Found | `FUND_NOT_FOUND` |
| `SubscriptionNotFoundException` | 404 Not Found | `SUBSCRIPTION_NOT_FOUND` |

Todas las respuestas de error siguen el formato:
```json
{
  "error": "CÓDIGO_ERROR",
  "message": "Descripción legible del error"
}
```

---

## Notificaciones (SES / SNS)

El sistema soporta dos canales de notificación, seleccionados según la preferencia del cliente (`notificationPreference`):

| Canal | Servicio AWS | Implementación |
|-------|-------------|----------------|
| **EMAIL** | Amazon SES | `EmailNotificationSender` — envía email con asunto y cuerpo formateado |
| **SMS** | Amazon SNS | `SmsNotificationSender` — envía SMS al número del cliente |

Las notificaciones se disparan automáticamente tras cada suscripción exitosa.

---

## Desarrollo Local

### Prerrequisitos

- Java 25+
- Maven 3.9+
- Docker y Docker Compose

### 1. Iniciar LocalStack

```bash
docker-compose up -d
```

Esto levanta LocalStack en `localhost:4566` con DynamoDB, SES y SNS.

### 2. Compilar y ejecutar tests

```bash
./mvnw clean test
```

### 3. Ejecutar la aplicación

```bash
export AWS_ACCESS_KEY_ID=dummy
export AWS_SECRET_ACCESS_KEY=dummy
export AWS_REGION=us-east-1
./mvnw spring-boot:run -Dspring-boot.run.profiles=localstack
```

La API estará disponible en `http://localhost:8080`.

### 4. Swagger UI

Acceder a `http://localhost:8080/swagger-ui/index.html` para explorar los endpoints interactivamente. Usar el botón **Authorize** (candado) para ingresar el token JWT.

---

## Despliegue en AWS con CloudFormation

### Prerrequisitos

1. **Cuenta AWS** con permisos de administrador o permisos para crear: DynamoDB, ECS, API Gateway, IAM, CloudWatch, EC2 (VPC/SG/ALB), SSM, ACM
2. **AWS CLI** configurado (`aws configure`)
3. **VPC existente** con al menos 2 subnets públicas
4. **Imagen Docker** del API publicada en Amazon ECR
5. **Certificado ACM** para HTTPS (opcional para pruebas)

### Paso 1: Construir y publicar la imagen Docker

```bash
# Compilar JAR
./mvnw clean package -DskipTests

# Crear repositorio ECR
aws ecr create-repository --repository-name btgpactual --region us-east-1

# Login en ECR
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com

# Build y push
docker build -t btgpactual .
docker tag btgpactual:latest <ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com/btgpactual:latest
docker push <ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com/btgpactual:latest
```

### Paso 2: Configurar el certificado ACM

```bash
aws ssm put-parameter \
  --name "/btgpactual/dev/certificate-arn" \
  --value "arn:aws:acm:us-east-1:<ACCOUNT_ID>:certificate/<CERT_ID>" \
  --type String \
  --overwrite
```

> **Nota:** Para pruebas sin HTTPS, modificar el Listener en el template a puerto 80/HTTP.

### Paso 3: Crear el stack de CloudFormation

```bash
aws cloudformation create-stack \
  --stack-name btgpactual-dev \
  --template-body file://infrastructure/cloudformation.yaml \
  --capabilities CAPABILITY_NAMED_IAM \
  --parameters \
    ParameterKey=Environment,ParameterValue=dev \
    ParameterKey=VpcId,ParameterValue=vpc-xxxxxxxxx \
    ParameterKey=SubnetIds,ParameterValue="subnet-aaa,subnet-bbb" \
    ParameterKey=ContainerImage,ParameterValue=<ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com/btgpactual:latest \
    ParameterKey=JwtSecret,ParameterValue="mi-clave-secreta-jwt-super-segura-256bits" \
    ParameterKey=SesFromEmail,ParameterValue=noreply@btgpactual.com \
    ParameterKey=DesiredTaskCount,ParameterValue=2
```

### Paso 4: Monitorear el despliegue

```bash
aws cloudformation describe-stacks --stack-name btgpactual-dev --query 'Stacks[0].StackStatus'
aws cloudformation wait stack-create-complete --stack-name btgpactual-dev
aws cloudformation describe-stacks --stack-name btgpactual-dev --query 'Stacks[0].Outputs'
```

### Paso 5: Verificar el despliegue

```bash
API_URL=$(aws cloudformation describe-stacks --stack-name btgpactual-dev \
  --query 'Stacks[0].Outputs[?OutputKey==`ApiGatewayUrl`].OutputValue' --output text)

curl $API_URL/actuator/health
curl -X POST $API_URL/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"cliente1","password":"password123"}'
```

### Paso 6: Verificar SES (email)

```bash
aws ses verify-email-identity --email-address noreply@btgpactual.com
aws ses verify-email-identity --email-address demo@btgpactual.com
```

### Actualizar el stack

```bash
aws cloudformation update-stack \
  --stack-name btgpactual-dev \
  --template-body file://infrastructure/cloudformation.yaml \
  --capabilities CAPABILITY_NAMED_IAM \
  --parameters \
    ParameterKey=Environment,UsePreviousValue=true \
    ParameterKey=VpcId,UsePreviousValue=true \
    ParameterKey=SubnetIds,UsePreviousValue=true \
    ParameterKey=ContainerImage,ParameterValue=<ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com/btgpactual:v2 \
    ParameterKey=JwtSecret,UsePreviousValue=true \
    ParameterKey=SesFromEmail,UsePreviousValue=true \
    ParameterKey=DesiredTaskCount,UsePreviousValue=true
```

### Eliminar el stack

```bash
aws cloudformation delete-stack --stack-name btgpactual-dev
aws cloudformation wait stack-delete-complete --stack-name btgpactual-dev
```

> **Nota:** Las tablas DynamoDB tienen `DeletionPolicy: Retain` y no se eliminarán automáticamente. Para eliminarlas manualmente:
> ```bash
> aws dynamodb delete-table --table-name Clients-dev
> aws dynamodb delete-table --table-name Funds-dev
> aws dynamodb delete-table --table-name Transactions-dev
> aws dynamodb delete-table --table-name Subscriptions-dev
> aws dynamodb delete-table --table-name Users-dev
> ```

---

## Recursos Creados por CloudFormation

| Recurso | Tipo | Descripción |
|---------|------|-------------|
| Clients | DynamoDB Table | Clientes (PK: clientId) |
| Funds | DynamoDB Table | Fondos de inversión (PK: fundId) |
| Transactions | DynamoDB Table | Transacciones con GSIs clientId-index, subscriptionId-index |
| Subscriptions | DynamoDB Table | Suscripciones activas (PK: clientId, SK: fundId) |
| Users | DynamoDB Table | Usuarios autenticación (PK: userId, GSI: username-index) |
| ECS Cluster | ECS | Cluster Fargate con Container Insights |
| Task Definition | ECS | 512 CPU / 1024 MB, Java 25 |
| ECS Service | ECS | Servicio con ALB integration |
| ALB | Load Balancer | Application Load Balancer HTTPS |
| API Gateway | HTTP API | Punto de entrada con CORS y logging |
| IAM Roles | IAM | Execution role + Task role (DynamoDB, SES, SNS) |
| CloudWatch Logs | Logs | Logs de ECS y API Gateway (30 días retención) |

---

## Tests

El proyecto cuenta con **73 tests unitarios** distribuidos en las siguientes capas:

| Capa | Tests | Cobertura |
|------|-------|-----------|
| Controllers | 17 | Flujos HTTP exitosos y fallidos, validación de ownership |
| Services | 36 | Lógica de negocio completa: auth, suscripciones, transacciones, notificaciones |
| Security | 10 | JWT (generación, validación, expiración), ownership (CLIENT vs ADMIN) |
| Models | 9 | Builders y consistencia de campos |
| Integration | 1 | Context load (requiere `SPRING_BOOT_INTEGRATION_TESTS=true`) |

```bash
# Ejecutar todos los tests
./mvnw clean test

# Ejecutar tests con verbose
./mvnw test -Dsurefire.useFile=false
```

---

## Punto 2 — Query SQL (Álgebra Relacional)

El directorio [`2do punto/`](2do%20punto/) contiene un ejercicio independiente de SQL sobre un modelo relacional (SQL Server):

### Modelo Relacional

El archivo [`2do punto/Creación de tablas.sql`](2do%20punto/Creación%20de%20tablas.sql) define las siguientes tablas:

| Tabla | Descripción |
|-------|-------------|
| `Cliente` | Clientes (id, nombre, apellidos, ciudad) |
| `Sucursal` | Sucursales (id, nombre, ciudad) |
| `Producto` | Productos financieros (id, nombre, tipoProducto) |
| `Inscripcion` | Relación Cliente ↔ Producto (idProducto, idCliente) |
| `Disponibilidad` | Relación Sucursal ↔ Producto (idSucursal, idProducto) |
| `Visitan` | Relación Sucursal ↔ Cliente con fecha (idSucursal, idCliente, fechaVisita) |

### Consulta SQL

El archivo [`2do punto/Query.sql`](2do%20punto/Query.sql) resuelve el siguiente problema de **división relacional**:

> **Obtener los clientes que han visitado TODAS las sucursales donde están disponibles los productos a los que están inscritos.**

```sql
SELECT DISTINCT c.nombre, c.apellidos
FROM Cliente c
JOIN Inscripcion i ON c.id = i.idCliente
WHERE NOT EXISTS (
    SELECT 1 
    FROM Disponibilidad d
    WHERE d.idProducto = i.idProducto
    AND NOT EXISTS (
        SELECT 1 
        FROM Visitan v 
        WHERE v.idCliente = c.id 
        AND v.idSucursal = d.idSucursal
    )
);
```

**Lógica:** Utiliza el patrón de doble negación (`NOT EXISTS ... NOT EXISTS`) para implementar la división relacional — selecciona clientes para los cuales **no existe** ninguna sucursal con disponibilidad de sus productos inscritos que **no hayan visitado**.
