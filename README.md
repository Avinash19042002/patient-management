# Patient Management System

A microservices-based patient management application built with Spring Boot, featuring JWT authentication, gRPC inter-service communication, and event-driven architecture with Apache Kafka.

## Architecture

```
                         ┌──────────────────┐
                         │   API Gateway    │
                         │    (Port 4004)   │
                         └──┬──────────┬────┘
                            │          │
               JWT Validate │          │ Route
                            │          │
                 ┌──────────▼──┐  ┌────▼──────────────┐
                 │ Auth Service │  │  Patient Service   │
                 │  (Port 4005) │  │   (Port 4000)      │
                 │  [auth_db]   │  │   [patient_db]     │
                 └──────────────┘  └──┬─────────┬───────┘
                                      │         │
                              gRPC    │         │  Kafka
                                      │         │  (Topic: patients)
                            ┌─────────▼──┐  ┌───▼──────────────┐
                            │  Billing   │  │ Analytics Service │
                            │  Service   │  │   (Port 4002)    │
                            │ (Port 4001)│  │                  │
                            │ (gRPC 9001)│  │                  │
                            └────────────┘  └──────────────────┘
```

### Services

| Service | Port | Description |
|---------|------|-------------|
| **API Gateway** | 4004 | Spring Cloud Gateway — routes requests, validates JWT tokens |
| **Auth Service** | 4005 | Handles user login and JWT token generation/validation |
| **Patient Service** | 4000 | CRUD operations for patients, publishes Kafka events |
| **Billing Service** | 4001 (HTTP), 9001 (gRPC) | Billing operations exposed via gRPC |
| **Analytics Service** | 4002 | Consumes patient events from Kafka for analytics |

### Communication Patterns

- **Client → API Gateway**: REST over HTTP
- **API Gateway → Auth Service**: REST (JWT validation via `/validate` endpoint)
- **API Gateway → Patient Service**: REST (with JWT filter)
- **Patient Service → Billing Service**: Synchronous gRPC
- **Patient Service → Analytics Service**: Asynchronous via Kafka (`patients` topic, Protobuf-serialized)

## Tech Stack

- **Java 17** / **Spring Boot 3.x**
- **Spring Cloud Gateway** (API routing)
- **Spring Data JPA** + **PostgreSQL** (persistence)
- **Spring Security** + **JJWT** (authentication)
- **gRPC** + **Protobuf** (inter-service sync communication)
- **Apache Kafka** (event-driven async communication)
- **SpringDoc OpenAPI** (API documentation)
- **Docker** + **Kubernetes** (containerization and orchestration)

## Prerequisites

- Java 17+
- Maven 3.9+
- Docker Desktop
- Minikube and kubectl (for Kubernetes deployment)

## Project Structure

```
patient-management/
├── api-gateway/              # Spring Cloud Gateway
├── auth-service/             # JWT authentication service
├── patient-service/          # Patient CRUD + Kafka producer + gRPC client
├── billing-service/          # gRPC billing server
├── analytics-service/        # Kafka consumer for patient events
├── integration-tests/        # REST Assured integration tests
├── api-requests/             # HTTP request files for manual testing
└── k8s/                      # Kubernetes manifests
    ├── namespace.yaml
    ├── configmap.yaml
    ├── secrets.yaml
    ├── infrastructure/       # PostgreSQL, Zookeeper, Kafka
    └── services/             # Microservice deployments and services
```

## API Reference

All requests go through the API Gateway at `http://localhost:4004`.

### Authentication

#### Login

```http
POST /auth/login
Content-Type: application/json

{
  "email": "testuser@test.com",
  "password": "password"
}
```

Returns a JWT token:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

#### Validate Token

```http
GET /auth/validate
Authorization: Bearer <token>
```

Returns `200 OK` if valid, `401 Unauthorized` if not.

### Patients (JWT required)

All patient endpoints require the `Authorization: Bearer <token>` header.

#### List Patients

```http
GET /api/patients
Authorization: Bearer <token>
```

#### Create Patient

```http
POST /api/patients
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "John Doe",
  "email": "john@example.com",
  "address": "123 Main St",
  "dateOfBirth": "1990-01-15",
  "gender": "Male",
  "registeredDate": "2024-01-10"
}
```

#### Update Patient

```http
PUT /api/patients/{id}
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "John Updated",
  "email": "john.updated@example.com",
  "address": "456 New St",
  "dateOfBirth": "1990-01-15",
  "gender": "Male"
}
```

#### Delete Patient

```http
DELETE /api/patients/{id}
Authorization: Bearer <token>
```

### OpenAPI Documentation

- Patient Service docs: `GET /api-docs/patients`
- Auth Service docs: `GET /api-docs/auth`

## Deployment on Kubernetes (Minikube)

### 1. Start Minikube

```bash
minikube start --cpus=4 --memory=8192 --driver=docker
```

### 2. Build and Push Docker Images

```bash
docker login

docker build -t <dockerhub-username>/pm-api-gateway ./api-gateway
docker build -t <dockerhub-username>/pm-auth-service ./auth-service
docker build -t <dockerhub-username>/patient-service ./patient-service
docker build -t <dockerhub-username>/pm-billing-service ./billing-service
docker build -t <dockerhub-username>/pm-analytics-service ./analytics-service

docker push <dockerhub-username>/pm-api-gateway
docker push <dockerhub-username>/pm-auth-service
docker push <dockerhub-username>/patient-service
docker push <dockerhub-username>/pm-billing-service
docker push <dockerhub-username>/pm-analytics-service
```

### 3. Deploy to Kubernetes

```bash
# Namespace, config, and secrets
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/secrets.yaml
kubectl apply -f k8s/configmap.yaml

# Infrastructure (deploy in order, wait for each to be ready)
kubectl apply -f k8s/infrastructure/postgres-pv.yaml
kubectl apply -f k8s/infrastructure/pg_configmap.yaml
kubectl apply -f k8s/infrastructure/postgres-deployment.yaml
kubectl apply -f k8s/infrastructure/postgres-service.yaml
kubectl wait --for=condition=ready pod -l app=postgres -n patient-management --timeout=120s

kubectl apply -f k8s/infrastructure/zookeeper-deployment.yaml
kubectl apply -f k8s/infrastructure/zookeeper-service.yaml
kubectl wait --for=condition=ready pod -l app=zookeeper -n patient-management --timeout=120s

kubectl apply -f k8s/infrastructure/kafka-deployment.yaml
kubectl apply -f k8s/infrastructure/kafka-service.yaml
kubectl wait --for=condition=ready pod -l app=kafka -n patient-management --timeout=120s

# All microservices
kubectl apply -f k8s/services/
```

### 4. Verify

```bash
kubectl get pods -n patient-management
```

All pods should show `Running` status:

```
analytics-service-xxx   1/1   Running
api-gateway-xxx         1/1   Running
auth-service-xxx        1/1   Running
billing-service-xxx     1/1   Running
kafka-xxx               1/1   Running
patient-service-xxx     1/1   Running
postgres-xxx            1/1   Running
zookeeper-xxx           1/1   Running
```

### 5. Access the Application

**Option A — Port forward:**

```bash
kubectl port-forward svc/api-gateway 4004:4004 -n patient-management
# Access at http://localhost:4004
```

**Option B — Minikube service:**

```bash
minikube service api-gateway -n patient-management --url
```

## Database

Two isolated PostgreSQL databases are created automatically:

| Database | Used by | Tables |
|----------|---------|--------|
| `auth_db` | auth-service | `users` |
| `patient_db` | patient-service | `patient` |

### Seed Data

**auth-service** seeds a default admin user:
- Email: `testuser@test.com`
- Password: `password`
- Role: `ADMIN`

**patient-service** seeds 15 sample patient records on startup.

## Environment Variables

### auth-service

| Variable | Description |
|----------|-------------|
| `SPRING_DATASOURCE_URL` | JDBC URL for auth_db |
| `SPRING_DATASOURCE_USERNAME` | Database username |
| `SPRING_DATASOURCE_PASSWORD` | Database password |
| `JWT_SECRET` | Base64-encoded HMAC key for JWT signing |

### patient-service

| Variable | Description |
|----------|-------------|
| `SPRING_DATASOURCE_URL` | JDBC URL for patient_db |
| `SPRING_DATASOURCE_USERNAME` | Database username |
| `SPRING_DATASOURCE_PASSWORD` | Database password |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | Kafka broker address |
| `BILLING_SERVICE_ADDRESS` | Billing service hostname |
| `BILLING_SERVICE_GRPC_PORT` | Billing gRPC port (default: 9001) |

### analytics-service

| Variable | Description |
|----------|-------------|
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | Kafka broker address |

### api-gateway

| Variable | Description |
|----------|-------------|
| `AUTH_SERVICE_URL` | Auth service base URL for JWT validation |
