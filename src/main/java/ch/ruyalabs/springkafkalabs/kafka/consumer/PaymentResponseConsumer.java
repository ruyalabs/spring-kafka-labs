package ch.ruyalabs.springkafkalabs.kafka.consumer;

import ch.ruyalabs.types.PaymentDisbursementResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudevents.CloudEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentResponseConsumer {

    private static final Logger logger = LoggerFactory.getLogger(PaymentResponseConsumer.class);

    private final ObjectMapper objectMapper;

    public PaymentResponseConsumer() {
        this.objectMapper = new ObjectMapper();
    }

    @KafkaListener(topics = "${payment.kafka.topics.response}")
    public void handlePaymentResponse(ConsumerRecord<String, CloudEvent> record) {
        try {
            CloudEvent cloudEvent = record.value();

            //
            if (cloudEvent == null) {
                logger.error("Received message with deserialization error from topic: {}, partition: {}, offset: {}, key: {}", 
                    record.topic(), record.partition(), record.offset(), record.key());

                // Log specific Spring Kafka ErrorHandlingDeserializer headers
                logDeserializationErrorHeaders(record);

                // Skip processing this message but continue with the next one
                return;
            }

            logAllHeaders(record);

            logger.info("Received CloudEvent with ID: {}, Type: {}, Source: {}", 
                       cloudEvent.getId(), cloudEvent.getType(), cloudEvent.getSource());

            if (cloudEvent.getData() != null) {
                PaymentDisbursementResponse response = objectMapper.readValue(
                    cloudEvent.getData().toBytes(), 
                    PaymentDisbursementResponse.class
                );

                logger.info("Processing payment response for disbursementId: {}, status: {}", 
                           response.getDisbursementId(), response.getStatus());

                processPaymentResponse(response);
            } else {
                logger.warn("Received CloudEvent with no data");
            }
        } catch (Exception e) {
            logger.error("Error processing payment response CloudEvent from topic: {}, partition: {}, offset: {}, key: {}, error: {}", 
                record.topic(), record.partition(), record.offset(), record.key(), e.getMessage(), e);
        }
    }

    private void logDeserializationErrorHeaders(ConsumerRecord<String, CloudEvent> record) {
        if (record.headers() != null) {
            // Log standard Spring Kafka ErrorHandlingDeserializer headers
            logSpecificErrorHeader(record, "spring.deserializer.exception.message", "Exception Message");
            logSpecificErrorHeader(record, "spring.deserializer.exception.stacktrace", "Exception Stacktrace");
            logSpecificErrorHeader(record, "spring.deserializer.exception.fqcn", "Exception Class");
            logSpecificErrorHeader(record, "spring.deserializer.key.exception.message", "Key Exception Message");
            logSpecificErrorHeader(record, "spring.deserializer.key.exception.stacktrace", "Key Exception Stacktrace");
            logSpecificErrorHeader(record, "spring.deserializer.key.exception.fqcn", "Key Exception Class");
            logSpecificErrorHeader(record, "spring.deserializer.value.exception.message", "Value Exception Message");
            logSpecificErrorHeader(record, "spring.deserializer.value.exception.stacktrace", "Value Exception Stacktrace");
            logSpecificErrorHeader(record, "spring.deserializer.value.exception.fqcn", "Value Exception Class");

            // Log any other headers that might contain error information
            record.headers().forEach(header -> {
                String headerKey = header.key();
                if ((headerKey.contains("deserializer") || headerKey.contains("error")) && 
                    !headerKey.startsWith("spring.deserializer.")) {
                    String headerValue = header.value() != null ? new String(header.value()) : "null";
                    logger.error("Additional deserialization error header - {}: {}", headerKey, headerValue);
                }
            });
        }
    }

    private void logSpecificErrorHeader(ConsumerRecord<String, CloudEvent> record, String headerKey, String description) {
        Header header = record.headers().lastHeader(headerKey);
        if (header != null && header.value() != null) {
            String headerValue = new String(header.value());
            logger.error("Deserialization error - {}: {}", description, headerValue);
        }
    }

    private void logAllHeaders(ConsumerRecord<String, CloudEvent> record) {
        if (record.headers() != null && record.headers().iterator().hasNext()) {
            logger.info("Kafka message headers for topic: {}, partition: {}, offset: {}", 
                       record.topic(), record.partition(), record.offset());

            for (Header header : record.headers()) {
                String headerValue = header.value() != null ? new String(header.value()) : "null";
                logger.info("Header - Key: {}, Value: {}", header.key(), headerValue);
            }
        } else {
            logger.info("No Kafka headers found for message on topic: {}, partition: {}, offset: {}", 
                       record.topic(), record.partition(), record.offset());
        }
    }

    private void processPaymentResponse(PaymentDisbursementResponse response) {
        switch (response.getStatus()) {
            case PROCESSED:
                logger.info("Payment processed successfully for disbursementId: {}, transactionId: {}", 
                           response.getDisbursementId(), response.getTransactionId());
                break;
            case FAILED:
                logger.error("Payment failed for disbursementId: {}, reason: {}", 
                            response.getDisbursementId(), response.getFailureReason());
                break;
            case PENDING:
                logger.info("Payment is pending for disbursementId: {}", 
                           response.getDisbursementId());
                break;
            default:
                logger.warn("Unknown payment status: {} for disbursementId: {}", 
                           response.getStatus(), response.getDisbursementId());
        }
    }
}
