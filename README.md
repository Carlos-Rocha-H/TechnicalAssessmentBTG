# BTG Pactual — Investment Fund Self-Service API

API REST de autogestión de fondos de inversión para clientes BTG Pactual. Permite suscribirse a fondos, cancelar suscripciones, consultar historial de transacciones y recibir notificaciones por email/SMS.

## Stack Tecnológico

- **Java 25** + **Spring Boot 4.0.3**
- **AWS DynamoDB** — Persistencia NoSQL
- **AWS SES / SNS** — Notificaciones email y SMS
- **Spring Security + JWT** — Autenticación y autorización
- **ECS Fargate** — Contenedorización serverless
- **API Gateway** — Punto de entrada HTTP
- **CloudFormation** — Infraestructura como código

## Estructura del Proyecto

```
btgpactual/
├── infrastructure/
│   └── cloudformation.yaml          # Template CloudFormation
├── docs/
│   └── historias-de-usuario.md      # Historias de usuario
├── docker-compose.yml               # LocalStack para desarrollo local
├── src/main/java/.../
│   ├── config/                      # DataSeeder, AwsConfig, OpenApiConfig, SecurityConfig
│   ├── controller/                  # SubscriptionController, TransactionController, AuthController
│   ├── model/                       # Client, Fund, Subscription, Transaction, User
│   ├── repository/                  # Repositorios DynamoDB
│   ├── service/                     # Lógica de negocio y notificaciones
│   ├── security/                    # JWT, filtros, ownership validation
│   └── exception/                   # Excepciones y handler global
└── src/test/java/.../               # 64 tests unitarios
```

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

Acceder a `http://localhost:8080/swagger-ui/index.html` para explorar los endpoints. Usar el candado (Authorize) para ingresar el token JWT.

### 5. Autenticación

```bash
# Login (obtener token)
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"cliente1","password":"password123"}'

# Usar token en requests
curl -H "Authorization: Bearer <TOKEN>" \
  http://localhost:8080/api/v1/clients/{clientId}/transactions
```

**Usuarios demo:**

| Usuario | Password | Rol | ClientId |
|---------|----------|-----|----------|
| cliente1 | password123 | CLIENT | 550e8400-e29b-41d4-a716-446655440000 |
| admin | admin123 | ADMIN | — |

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
# Crear Dockerfile si no existe
cat > Dockerfile <<'EOF'
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY target/btgpactual-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=aws"]
EOF

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

Antes de desplegar, actualizar el parámetro SSM del certificado:

```bash
# Si ya tienes un certificado ACM
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
# Ver estado del stack
aws cloudformation describe-stacks --stack-name btgpactual-dev --query 'Stacks[0].StackStatus'

# Esperar a que termine
aws cloudformation wait stack-create-complete --stack-name btgpactual-dev

# Ver outputs (URL del API, etc.)
aws cloudformation describe-stacks --stack-name btgpactual-dev --query 'Stacks[0].Outputs'
```

### Paso 5: Verificar el despliegue

```bash
# Obtener URL del API Gateway
API_URL=$(aws cloudformation describe-stacks --stack-name btgpactual-dev \
  --query 'Stacks[0].Outputs[?OutputKey==`ApiGatewayUrl`].OutputValue' --output text)

# Health check
curl $API_URL/actuator/health

# Login
curl -X POST $API_URL/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"cliente1","password":"password123"}'
```

### Paso 6: Verificar SES (email)

```bash
# Verificar email en SES (requerido en modo sandbox)
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

> **Nota:** Las tablas DynamoDB tienen `DeletionPolicy: Retain` y no se eliminarán automáticamente para proteger los datos. Para eliminarlas manualmente:
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

## Endpoints API

| Método | Endpoint | Descripción | Auth |
|--------|----------|-------------|------|
| POST | `/api/v1/auth/login` | Obtener token JWT | No |
| POST | `/api/v1/clients/{clientId}/subscriptions` | Suscribirse a fondo | JWT |
| DELETE | `/api/v1/clients/{clientId}/subscriptions/{fundId}` | Cancelar suscripción | JWT |
| GET | `/api/v1/clients/{clientId}/transactions` | Historial de transacciones | JWT |

## Tests

```bash
# Ejecutar todos los tests (64 tests)
./mvnw clean test

# Ejecutar tests con verbose
./mvnw test -Dsurefire.useFile=false
```
