# Spring Kafka ErrorHandlingDeserializer Implementation

## Overview

This project implements Spring Kafka's `ErrorHandlingDeserializer` to gracefully handle deserialization errors when consuming CloudEvent messages. The ErrorHandlingDeserializer acts as a wrapper around the real deserializer and provides robust error handling capabilities.

## How ErrorHandlingDeserializer Works

### Basic Mechanism

1. **Wraps the Real Deserializer**: The ErrorHandlingDeserializer wraps the actual deserializer (CloudEventDeserializer in our case)
2. **Catches Exceptions**: If the real deserializer fails and throws an exception, the ErrorHandlingDeserializer catches it
3. **Returns Null**: Instead of letting the exception propagate (which would stop the listener invocation), it returns null as the message value
4. **Adds Error Headers**: It adds the exception details into the message headers with specific keys

### Configuration

In `KafkaConsumerConfig.java`:

```java
// Configure error handling deserializers
configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
configProps.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
configProps.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, CloudEventDeserializer.class);
```

## Error Headers

When deserialization fails, ErrorHandlingDeserializer adds the following standard headers:

### General Exception Headers
- `spring.deserializer.exception.message` - The exception message
- `spring.deserializer.exception.stacktrace` - The full stack trace
- `spring.deserializer.exception.fqcn` - Fully qualified class name of the exception

### Key-Specific Exception Headers
- `spring.deserializer.key.exception.message` - Key deserialization exception message
- `spring.deserializer.key.exception.stacktrace` - Key deserialization stack trace
- `spring.deserializer.key.exception.fqcn` - Key deserialization exception class

### Value-Specific Exception Headers
- `spring.deserializer.value.exception.message` - Value deserialization exception message
- `spring.deserializer.value.exception.stacktrace` - Value deserialization stack trace
- `spring.deserializer.value.exception.fqcn` - Value deserialization exception class

## Consumer Implementation

In `PaymentResponseConsumer.java`, the consumer handles deserialization errors as follows:

### Null Value Handling
```java
// Handle deserialization errors - cloudEvent will be null if deserialization failed
if (cloudEvent == null) {
    logger.error("Received message with deserialization error from topic: {}, partition: {}, offset: {}, key: {}", 
        record.topic(), record.partition(), record.offset(), record.key());

    // Log specific Spring Kafka ErrorHandlingDeserializer headers
    logDeserializationErrorHeaders(record);

    // Skip processing this message but continue with the next one
    return;
}
```

### Enhanced Error Header Logging
The `logDeserializationErrorHeaders` method specifically looks for and logs all standard Spring Kafka error headers:

```java
private void logDeserializationErrorHeaders(ConsumerRecord<String, CloudEvent> record) {
    if (record.headers() != null) {
        // Log standard Spring Kafka ErrorHandlingDeserializer headers
        logSpecificErrorHeader(record, "spring.deserializer.exception.message", "Exception Message");
        logSpecificErrorHeader(record, "spring.deserializer.exception.stacktrace", "Exception Stacktrace");
        logSpecificErrorHeader(record, "spring.deserializer.exception.fqcn", "Exception Class");
        // ... and more
    }
}
```

## Benefits

1. **Resilience**: The application continues processing other messages even when some messages fail to deserialize
2. **Observability**: Detailed error information is logged, making debugging easier
3. **Non-blocking**: Deserialization errors don't stop the entire consumer
4. **Comprehensive Error Tracking**: All error details are preserved in headers for analysis

## Example Scenarios

### Malformed JSON
When a malformed JSON CloudEvent is received:
- ErrorHandlingDeserializer catches the JsonParseException
- Returns null for the CloudEvent value
- Adds error headers with exception details
- Consumer logs the error and continues processing

### Invalid CloudEvent Structure
When a JSON message doesn't conform to CloudEvent schema:
- CloudEventDeserializer throws a deserialization exception
- ErrorHandlingDeserializer catches it and returns null
- Error headers contain the specific validation failure details
- Consumer handles gracefully and continues

### Network/Encoding Issues
When message encoding or network issues cause corruption:
- The underlying deserializer fails
- ErrorHandlingDeserializer provides fallback behavior
- Error details are preserved for troubleshooting
- System remains operational

## Testing

The implementation includes comprehensive tests in `DeserializerErrorHandlingTest.java`:

- `testConsumerHandlesNullCloudEventGracefully()` - Tests basic null handling
- `testConsumerHandlesSpringKafkaErrorHeaders()` - Tests standard error header processing
- `testConsumerHandlesCustomErrorHeaders()` - Tests custom error header handling
- `testSendMalformedJsonMessage()` - Tests malformed message scenarios

## Monitoring and Alerting

Consider implementing monitoring for:
- Frequency of deserialization errors
- Types of exceptions occurring
- Topics/partitions with high error rates
- Error header patterns for proactive issue detection

This robust error handling ensures that the payment processing system remains resilient and observable even when encountering problematic messages.