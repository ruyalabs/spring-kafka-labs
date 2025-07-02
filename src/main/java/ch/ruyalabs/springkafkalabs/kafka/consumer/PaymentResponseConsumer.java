package ch.ruyalabs.springkafkalabs.kafka.consumer;

import ch.ruyalabs.types.PaymentDisbursementResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.format.EventFormat;
import io.cloudevents.jackson.JsonFormat;
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
    public void handlePaymentResponse(ConsumerRecord<String, String> record) {
        try {
            String rawMessage = record.value();

            // Handle null or empty messages
            if (rawMessage == null || rawMessage.trim().isEmpty()) {
                logger.error("Received null or empty message from topic: {}, partition: {}, offset: {}, key: {}", 
                    record.topic(), record.partition(), record.offset(), record.key());
                return;
            }

            // Check whether the received Cloud Event has been sent in structured mode
            if (!isStructuredMode(record)) {
                logger.error("CloudEvent not sent in structured mode (Content-Type: application/cloudevents+json; charset=UTF-8) from topic: {}, partition: {}, offset: {}, key: {}", 
                    record.topic(), record.partition(), record.offset(), record.key());
                return;
            }

            // Deserialize the received Cloud Event to CloudEvent (String -> CloudEvent)
            CloudEvent cloudEvent = deserializeCloudEvent(rawMessage);
            if (cloudEvent == null) {
                logger.error("Failed to deserialize CloudEvent from topic: {}, partition: {}, offset: {}, key: {}", 
                    record.topic(), record.partition(), record.offset(), record.key());
                return;
            }

            logger.info("Received CloudEvent with ID: {}, Type: {}, Source: {}", 
                       cloudEvent.getId(), cloudEvent.getType(), cloudEvent.getSource());

            // Check whether the cloud event attributes were delivered as defined in the schema
            if (!validateCloudEventAttributes(cloudEvent)) {
                logger.error("CloudEvent attributes validation failed from topic: {}, partition: {}, offset: {}, key: {}", 
                    record.topic(), record.partition(), record.offset(), record.key());
                return;
            }

            // Map cloud event data to generated class
            if (cloudEvent.getData() != null) {
                PaymentDisbursementResponse response = objectMapper.readValue(
                    cloudEvent.getData().toBytes(), 
                    PaymentDisbursementResponse.class
                );

                logger.info("Processing payment response for disbursementId: {}, status: {}", 
                           response.getDisbursementId(), response.getStatus());

                // Process payment response
                processPaymentResponse(response);
            } else {
                logger.warn("Received CloudEvent with no data");
            }
        } catch (Exception e) {
            logger.error("Error processing payment response CloudEvent from topic: {}, partition: {}, offset: {}, key: {}, error: {}", 
                record.topic(), record.partition(), record.offset(), record.key(), e.getMessage(), e);
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

    /**
     * Check whether the received Cloud Event has been sent in structured mode
     * (Content-Type: application/cloudevents+json; charset=UTF-8)
     */
    private boolean isStructuredMode(ConsumerRecord<String, String> record) {
        Header contentTypeHeader = record.headers().lastHeader("content-type");
        if (contentTypeHeader == null) {
            logger.debug("No content-type header found");
            return false;
        }

        String contentType = new String(contentTypeHeader.value());
        logger.debug("Content-Type header: {}", contentType);

        // Check for structured mode content type
        return contentType.toLowerCase().contains("application/cloudevents+json");
    }

    /**
     * Deserialize the received Cloud Event string to CloudEvent object
     */
    private CloudEvent deserializeCloudEvent(String rawMessage) {
        try {
            EventFormat eventFormat = new JsonFormat();
            return eventFormat.deserialize(rawMessage.getBytes());
        } catch (Exception e) {
            logger.error("Failed to deserialize CloudEvent: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Validate cloud event attributes against the schema requirements
     */
    private boolean validateCloudEventAttributes(CloudEvent cloudEvent) {
        // Check specversion
        if (!"1.0".equals(cloudEvent.getSpecVersion().toString())) {
            logger.error("Invalid specversion: expected '1.0', got '{}'", cloudEvent.getSpecVersion());
            return false;
        }

        // Check type
        if (!"com.ruyalabs.payment.disbursement.request".equals(cloudEvent.getType())) {
            logger.error("Invalid type: expected 'com.ruyalabs.payment.disbursement.request', got '{}'", cloudEvent.getType());
            return false;
        }

        // Check source
        String source = cloudEvent.getSource().toString();
        if (!"payment-service".equals(source) && !"payment-2-service".equals(source)) {
            logger.error("Invalid source: expected 'payment-service' or 'payment-2-service', got '{}'", source);
            return false;
        }

        // Check id is present and not empty
        if (cloudEvent.getId() == null || cloudEvent.getId().trim().isEmpty()) {
            logger.error("Invalid id: id is required and cannot be empty");
            return false;
        }

        // Check datacontenttype
        if (cloudEvent.getDataContentType() != null && 
            !"application/json".equals(cloudEvent.getDataContentType())) {
            logger.error("Invalid datacontenttype: expected 'application/json', got '{}'", cloudEvent.getDataContentType());
            return false;
        }

        // Check data is present
        if (cloudEvent.getData() == null) {
            logger.error("Invalid data: data is required");
            return false;
        }

        logger.debug("CloudEvent attributes validation passed");
        return true;
    }
}
