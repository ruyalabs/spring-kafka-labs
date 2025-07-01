# Spring Kafka Labs

A comprehensive Spring Boot application demonstrating advanced Kafka integration patterns with CloudEvents, error handling, and payment processing workflows.

## Overview

This project showcases a production-ready payment processing system built with Spring Kafka, featuring:

- **CloudEvents Integration**: Standards-compliant event messaging using CloudEvents specification
- **Robust Error Handling**: Advanced deserialization error handling with Spring Kafka's ErrorHandlingDeserializer
- **High Availability**: 3-node Kafka cluster with KRaft mode (no Zookeeper dependency)
- **Payment Processing**: Complete payment disbursement workflow with REST API triggers
- **Schema Validation**: JSON schema validation for CloudEvent payloads
- **Monitoring**: Integrated Kafdrop UI for Kafka cluster monitoring

## Architecture

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   REST API      │    │   Kafka Cluster  │    │   Consumers     │
│                 │    │                  │    │                 │
│ PaymentController│───▶│ payment-requests │───▶│PaymentProcessor │
│                 │    │                  │    │                 │
│ POST /trigger   │    │ payment-responses│◀───│PaymentResponse  │
└─────────────────┘    └──────────────────┘    │   Consumer      │
                                               └─────────────────┘
```

## Features

### 🚀 Core Functionality
- **Payment Request Processing**: REST endpoint to trigger payment disbursements
- **CloudEvent Messaging**: Standards-compliant event structure with proper metadata
- **Asynchronous Processing**: Non-blocking payment processing with Kafka

### 🛡️ Error Handling
- **Deserialization Error Recovery**: Graceful handling of malformed messages
- **Comprehensive Error Logging**: Detailed error headers and stack traces
- **Circuit Breaker Pattern**: Prevents cascade failures in message processing

### 📊 Monitoring & Observability
- **Kafdrop Integration**: Web UI for Kafka cluster monitoring
- **Structured Logging**: Comprehensive logging with correlation IDs
- **Health Checks**: Built-in health monitoring for Kafka brokers

## Quick Start

### Prerequisites
- Java 17+
- Docker & Docker Compose
- Maven 3.6+

### 1. Start Kafka Cluster
```bash
docker-compose up -d
```

This starts:
- 3 Kafka brokers (ports 29092, 29093, 29094)
- Kafdrop UI (http://localhost:19000)

### 2. Build and Run Application
```bash
mvn clean install
mvn spring-boot:run
```

### 3. Trigger a Payment
```bash
curl -X POST http://localhost:8080/api/payment/trigger
```

### 4. Monitor with Kafdrop
Open http://localhost:19000 to view:
- Topic messages
- Consumer group status
- Broker health

## Configuration

### Kafka Topics
- `payment-requests`: Incoming payment disbursement requests
- `payment-responses`: Payment processing results

### Application Properties
```yaml
spring:
  kafka:
    bootstrap-servers: localhost:29092,localhost:29093,localhost:29094

payment:
  kafka:
    topics:
      request: payment-requests
      response: payment-responses
```

## API Endpoints

### Payment Processing
- **POST** `/api/payment/trigger` - Trigger a dummy payment request
  - Response: Payment disbursement ID and status

## Data Models

### PaymentDisbursementRequest
```json
{
  "disbursementId": "uuid",
  "recipient": {
    "name": "string",
    "email": "string",
    "bankDetails": {
      "accountNumber": "string",
      "sortCode": "string",
      "iban": "string"
    }
  },
  "amount": {
    "value": "number",
    "currency": "string"
  },
  "paymentMethod": "BANK_TRANSFER",
  "requestedAt": "datetime"
}
```

### CloudEvent Structure
All messages follow CloudEvents v1.0 specification:
```json
{
  "specversion": "1.0",
  "type": "com.ruyalabs.payment.disbursement.request",
  "source": "payment-service",
  "id": "uuid",
  "time": "2023-12-01T10:30:00Z",
  "datacontenttype": "application/json",
  "data": {
    "disbursementId": "550e8400-e29b-41d4-a716-446655440000",
    "recipient": {
      "name": "John Doe",
      "email": "john.doe@example.com"
    },
    "amount": {
      "value": 2500.50,
      "currency": "USD"
    }
  }
}
```

## Error Handling

The application implements Spring Kafka's `ErrorHandlingDeserializer` for robust message processing:

- **Graceful Degradation**: Malformed messages don't stop processing
- **Error Headers**: Detailed exception information in message headers
- **Logging**: Comprehensive error logging with context
- **Recovery**: Automatic retry with exponential backoff

For detailed error handling documentation, see [ErrorHandlingDeserializer Documentation](src/main/resources/docs/ErrorHandlingDeserializer-Documentation.md).

## Testing

Run the complete test suite:
```bash
mvn test
```

Test categories:
- **Integration Tests**: End-to-end Kafka message flow
- **Error Handling Tests**: Deserialization error scenarios
- **Schema Validation Tests**: CloudEvent structure validation

## Development

### Project Structure
```
src/
├── main/java/ch/ruyalabs/springkafkalabs/
│   ├── config/          # Kafka configuration
│   ├── controller/      # REST endpoints
│   ├── kafka/
│   │   ├── consumer/    # Message consumers
│   │   └── producer/    # Message producers
│   └── SpringKafkaLabsApplication.java
├── main/resources/
│   ├── json/schema/     # JSON schemas
│   └── application.yml
└── test/                # Test suites
```

### Adding New Features
1. Define message schemas in `src/main/resources/json/schema/`
2. Generate POJOs using `jsonschema2pojo-maven-plugin`
3. Implement producers in `kafka/producer/`
4. Implement consumers in `kafka/consumer/`
5. Add integration tests

## Monitoring

### Kafdrop Dashboard
Access the Kafka monitoring dashboard at http://localhost:19000

Features:
- Topic browser with message inspection
- Consumer group monitoring
- Broker health and configuration
- Partition details and offsets

### Application Logs
The application provides structured logging for:
- Message processing events
- Error conditions with full context
- Performance metrics
- Health check results

## Production Considerations

### Security
- Configure SASL/SSL for production Kafka clusters
- Implement proper authentication and authorization
- Use encrypted connections for sensitive payment data

### Scalability
- Increase partition count for higher throughput
- Scale consumer instances based on partition count
- Monitor consumer lag and adjust accordingly

### Reliability
- Configure appropriate replication factors
- Set up monitoring and alerting
- Implement dead letter queues for failed messages

## Contributing

1. Fork the repository
2. Create a feature branch
3. Add tests for new functionality
4. Ensure all tests pass
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.
