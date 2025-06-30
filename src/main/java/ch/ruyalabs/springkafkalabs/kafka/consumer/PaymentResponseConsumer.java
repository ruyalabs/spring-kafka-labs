package ch.ruyalabs.springkafkalabs.kafka.consumer;

import ch.ruyalabs.types.PaymentDisbursementResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudevents.CloudEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
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

            // Log all Kafka headers
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
            logger.error("Error processing payment response CloudEvent: {}", record.value() != null ? record.value().getId() : "unknown", e);
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
