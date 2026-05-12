# Order Management System (OrderMS)

Event-driven order management system built with Spring Boot, MongoDB, and RabbitMQ.

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Technologies](#technologies)
- [Project Structure](#project-structure)
- [Technical Decisions](#technical-decisions)
- [Setup and Execution](#setup-and-execution)
- [API Endpoints](#api-endpoints)
- [Data Model](#data-model)

## Overview

**OrderMS** is a microservice responsible for managing customer orders. The system consumes order creation events via RabbitMQ, persists data in MongoDB, and provides REST APIs for querying orders and calculating totals per customer.

### Key Features

- Order creation event consumption via RabbitMQ
- Order persistence in MongoDB
- Paginated order queries by customer
- Total order value calculation per customer using MongoDB Aggregation
- Automatic DTO mapping with MapStruct

## Architecture

### System Architecture

```
┌─────────────────┐
│   RabbitMQ      │
│  (Event Source) │
└────────┬────────┘
         │ OrderCreatedEvent
         ▼
┌─────────────────────────────────┐
│      OrderMS (Spring Boot)      │
│                                 │
│  ┌──────────────────────────┐  │
│  │  OrderCreatedListener    │  │
│  └──────────┬───────────────┘  │
│             │                   │
│             ▼                   │
│  ┌──────────────────────────┐  │
│  │    OrderMapper           │  │
│  │    (MapStruct)           │  │
│  └──────────┬───────────────┘  │
│             │                   │
│             ▼                   │
│  ┌──────────────────────────┐  │
│  │    OrderService          │  │
│  └──────────┬───────────────┘  │
│             │                   │
│             ▼                   │
│  ┌──────────────────────────┐  │
│  │   OrderRepository        │  │
│  └──────────┬───────────────┘  │
└─────────────┼───────────────────┘
              │
              ▼
      ┌──────────────┐
      │   MongoDB    │
      └──────────────┘
```

### Data Flow

1. **Creation Event**: External system publishes `OrderCreatedEvent` to RabbitMQ queue
2. **Consumption**: `OrderCreatedListener` consumes the message
3. **Mapping**: `OrderMapper` (MapStruct) converts `OrderCreatedEvent` → `Order`
4. **Persistence**: `OrderService` saves the order to MongoDB via `OrderRepository`
5. **Query**: REST APIs allow querying orders and totals

## Technologies

### Core

- **Java 21** - Programming language
- **Spring Boot 3.5.14** - Main framework
- **Maven** - Dependency management

### Persistence

- **MongoDB** - Document-oriented NoSQL database
- **Spring Data MongoDB** - MongoDB access abstraction

### Messaging

- **RabbitMQ** - Message broker for asynchronous communication
- **Spring AMQP** - RabbitMQ integration

### Mapping

- **MapStruct 1.5.5** - Compile-time mapping code generation

### Infrastructure

- **Docker Compose** - Container orchestration (MongoDB + RabbitMQ)

## Project Structure

```
src/main/java/tech/buildrun/btgpactual/orderms/
├── config/
│   └── RabittMqConfig.java           # RabbitMQ configuration (queues, converters)
├── controller/
│   ├── OrderController.java          # REST endpoints
│   └── dto/
│       ├── ApiResponse.java          # Standardized response wrapper
│       ├── OrderResponse.java        # Order response DTO
│       └── PaginationResponse.java   # Pagination DTO
├── entities/
│   ├── Order.java                    # Main order entity
│   └── OrderItem.java                # Order item
├── listener/
│   ├── OrderCreatedListener.java     # RabbitMQ consumer
│   └── dto/
│       ├── OrderCreatedEvent.java    # Order creation event
│       └── OrderItemEvent.java       # Event item
├── mapper/
│   └── OrderMapper.java              # MapStruct interface
├── repository/
│   └── OrderRepository.java          # Spring Data MongoDB repository
└── service/
    └── OrderService.java             # Business logic
```

## Technical Decisions

### 1. **MongoDB as Database**

**Rationale:**

- Schema flexibility for order model evolution
- Performance in aggregation queries (total calculations)
- Native support for nested documents (items within orders)
- Horizontal scalability

**Implementation:**

- Use of `@Document` for mapping
- Index on `customerId` to optimize queries
- `DECIMAL128` for monetary values (precision)

### 2. **RabbitMQ for Messaging**

**Rationale:**

- Decoupling between systems
- Message delivery guarantee
- Automatic retry support
- Industry standard for event-driven architectures

**Implementation:**

- Durable queue `btg-pactual-order-created`
- Automatic JSON conversion with `Jackson2JsonMessageConverter`
- `@RabbitListener` for declarative consumption

### 3. **MapStruct for Mapping**

**Rationale:**

- Superior performance (no reflection)
- Type-safe at compile-time
- Generated code is auditable
- Reduced boilerplate

**Implementation:**

```java
@Mapper(componentModel = "spring")
public interface OrderMapper {
  @Mapping(target = "total", expression = "java(calculateTotal(event))")
  Order toEntity(OrderCreatedEvent event);
}
```

### 4. **MongoDB Aggregation Framework**

**Rationale:**

- Total calculations directly in the database
- Superior performance compared to in-memory calculations
- Reduced network traffic

**Implementation:**

```java
var aggregations = newAggregation(
    match(Criteria.where("customerId").is(customerId)),
    group().sum("total").as("total")
);
```

### 5. **Field Normalization (clientId/customerId)**

**Rationale:**

- Compatibility with legacy systems
- Integration flexibility
- Avoids breaking contracts

**Implementation:**

- Event accepts both `clientId` and `customerId`
- Internal system uses only `customerId`

## Setup and Execution

### Prerequisites

- Java 21+
- Maven 3.8+
- Docker and Docker Compose

### 1. Start Infrastructure (MongoDB + RabbitMQ)

```bash
cd local
docker-compose up -d
```

**Available services:**

- MongoDB: `localhost:27017` (user: `admin`, password: `123`)
- RabbitMQ Management: `http://localhost:15672` (user: `guest`, password: `guest`)
- RabbitMQ AMQP: `localhost:5672`

### 2. Compile the Project

```bash
./mvnw clean compile
```

### 3. Run the Application

```bash
./mvnw spring-boot:run
```

The application will be available at `http://localhost:8080`

### 4. Health Check

```bash
curl http://localhost:8080/actuator/health
```

## API Endpoints

### List Orders by Customer

```http
GET /customers/{customerId}/orders?page=0&pageSize=10
```

**Parameters:**

- `customerId` (path) - Customer ID
- `page` (query, optional) - Page number (default: 0)
- `pageSize` (query, optional) - Page size (default: 10)

**Response:**

```json
{
  "summary": {
    "totalOrders": 2670.1
  },
  "data": [
    {
      "orderId": 1001,
      "customerId": 1,
      "total": 120.0
    },
    {
      "orderId": 1002,
      "customerId": 1,
      "total": 2550.1
    }
  ],
  "pagination": {
    "page": 0,
    "pageSize": 10,
    "totalElements": 2,
    "totalPages": 1
  }
}
```

## Data Model

### RabbitMQ Event: `OrderCreatedEvent`

```json
{
  "orderId": 1001,
  "customerId": 1,
  "items": [
    {
      "product": "lápis",
      "quantity": 100,
      "price": 1.1
    },
    {
      "product": "caderno",
      "quantity": 10,
      "price": 1.0
    }
  ]
}
```

### MongoDB Document: `Order`

```json
{
  "_id": 1001,
  "customerId": 1,
  "total": {
    "$numberDecimal": "120.00"
  },
  "items": [
    {
      "product": "lápis",
      "quantity": 100,
      "price": {
        "$numberDecimal": "1.10"
      }
    },
    {
      "product": "caderno",
      "quantity": 10,
      "price": {
        "$numberDecimal": "1.00"
      }
    }
  ],
  "_class": "tech.buildrun.btgpactual.orderms.entities.Order"
}
```

## Configuration

### application.properties

```properties
# Application
spring.application.name=orderms

# MongoDB
spring.data.mongodb.host=localhost
spring.data.mongodb.port=27017
spring.data.mongodb.authentication-database=admin
spring.data.mongodb.username=admin
spring.data.mongodb.password=123
spring.data.mongodb.database=orderms
spring.data.mongodb.auto-index-creation=true
```

### Docker Compose (local/docker-compose.yml)

```yaml
services:
  mongodb:
    image: mongo
    ports:
      - 27017:27017
    environment:
      - MONGO_INITDB_ROOT_USERNAME=admin
      - MONGO_INITDB_ROOT_PASSWORD=123

  rabbitmq:
    image: rabbitmq:3.13-management
    ports:
      - 15672:15672 # Management UI
      - 5672:5672 # AMQP
```

## Testing the System

### 1. Publish Event to RabbitMQ

Access RabbitMQ Management: `http://localhost:15672`

1. Go to **Queues** → `btg-pactual-order-created`
2. Click **Publish message**
3. Paste the payload:

```json
{
  "orderId": 1003,
  "customerId": 1,
  "items": [
    {
      "product": "mouse",
      "quantity": 1,
      "price": 250.0
    }
  ]
}
```

### 2. Query Orders

```bash
curl http://localhost:8080/customers/1/orders
```

## Future Improvements

- [ ] Implement unit and integration tests
- [ ] Add data validation with Bean Validation

## Contributing

1. Fork the project
2. Create a feature branch (`git checkout -b feature/new-feature`)
3. Commit your changes (`git commit -m 'Add new feature'`)
4. Push to the branch (`git push origin feature/new-feature`)
5. Open a Pull Request

## License

This project is under the MIT license.

---

**Built with Java 21**
