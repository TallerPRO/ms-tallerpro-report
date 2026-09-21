# ms-tallerpro-report

> **Estado: provisorio / en construcción.** Este README se irá completando a medida que avance el proyecto (faltan tests, CI/CD y detalles de despliegue final).

Microservicio de KPIs operativos y panel de reportería para **TallerPro**. Consume eventos del tópico Kafka `jobs.events` (fuente de verdad de `ms-tallerpro-jobs`) y expone endpoints de solo lectura para el panel de operaciones (roles **Admin** y **JefeTaller**).

## Qué hace

- Consume `jobs.events` de forma asíncrona (nunca llama de vuelta a `ms-tallerpro-jobs`).
- Calcula 3 KPIs (RF-13):
  - Órdenes por hora por taller (ventana deslizante).
  - Tiempo de permanencia promedio (`RECEPCIONADA` → `ENTREGADA`) por taller.
  - Órdenes activas por estado y por taller, en tiempo real.
- Expone endpoints REST de solo lectura para el panel (RF-14), protegidos por JWT de Azure AD (roles Admin y JefeTaller).
- Cumple el RNF de desfase máximo de 5s entre el evento Kafka y su reflejo en el panel (log de warning si se excede).

## Stack

Spring Boot 3.3 · Java 17 · Spring Kafka · Spring Data JPA · PostgreSQL · Flyway · Spring Security (OAuth2 Resource Server / JWT) · springdoc-openapi · Lombok

## Estructura del proyecto

```
src/main/java/com/tallerpro/report/
├── config/       # Kafka consumer, seguridad JWT, OpenAPI
├── controller/   # Endpoints REST de solo lectura
├── dto/          # JobEvent (payload Kafka) y DTOs de respuesta de KPIs
├── kafka/        # Listener de jobs.events
├── model/        # Entidades JPA (OrderEvent, OrderState, OrderStatus)
├── repository/   # Repositorios JPA con las queries de agregación
└── service/      # Ingesta de eventos + cálculo de KPIs
src/main/resources/
├── application.yml
└── db/migration/ # Migraciones Flyway
```

## Cómo correrlo localmente

### Requisitos

- Java 17+
- Maven 3.9+
- PostgreSQL corriendo (o usar el `docker-compose` de más abajo)
- Un broker Kafka accesible con el tópico `jobs.events`

### Variables de entorno principales

| Variable | Default | Descripción |
|---|---|---|
| `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USER` / `DB_PASSWORD` | `localhost` / `5432` / `tallerpro_report` / `tallerpro` / `tallerpro` | Conexión a PostgreSQL |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Broker(s) Kafka |
| `AZURE_TENANT_ID` | `tenant-id` | Tenant de Azure AD (issuer del JWT) |
| `AZURE_API_CLIENT_ID` | `api-client-id` | Client ID de la App Registration (audience del JWT), el mismo para todos los MS |
| `TALLERPRO_JWT_ENABLED` | `true` | `false` solo en desarrollo local sin tenant (usuario ficticio con `TALLERPRO_DEV_ROLES`, default `Admin`) |
| `SERVER_PORT` | `8085` | |

### Levantar

```bash
mvn spring-boot:run
```

La app queda en `http://localhost:8085`.

- Swagger UI: `http://localhost:8085/swagger-ui.html`
- Health: `http://localhost:8085/actuator/health`

### Con Docker

```bash
docker build -t ms-tallerpro-report .
```

Ver `compose-report-fragment.yml` para integrarlo en el `compose-apps.yml` (ec2-apps) junto al resto de microservicios, con su propia base PostgreSQL.

## Endpoints principales

Todos requieren `Authorization: Bearer <JWT>` con rol **Admin** o **JefeTaller** (validado contra Azure AD). Sin token → 401; otro rol → 403.

Ejecución local sin Postgres/Kafka: `./mvnw spring-boot:test-run -Dspring-boot.run.profiles=test "-Dspring-boot.run.arguments=--tallerpro.security.jwt-enabled=false"`

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/report/kpis` | Panel consolidado (los 3 KPIs) |
| GET | `/api/report/kpis/orders-per-hour` | Órdenes por hora por taller |
| GET | `/api/report/kpis/dwell-time` | Tiempo de permanencia promedio |
| GET | `/api/report/kpis/active-orders` | Órdenes activas por estado |

Parámetros comunes: `tallerId` (opcional, filtra por taller). `orders-per-hour` acepta `windowMinutes` (default 60). `dwell-time` acepta `since` (ISO-8601, default últimas 24h).

## Pendiente

- [ ] Tests (consumo Kafka, cálculo de KPIs, endpoints) — próximo paso
- [ ] Revisar valores reales de `issuer-uri` / `audiences` contra la App Registration definitiva
- [ ] CI/CD (GitHub Actions) hacia EC2
- [ ] Documentar OpenAPI final una vez cerrados los contratos con el frontend
