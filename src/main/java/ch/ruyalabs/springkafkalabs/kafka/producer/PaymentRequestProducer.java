package ch.ruyalabs.springkafkalabs.kafka.producer;

import ch.ruyalabs.types.PaymentDisbursementRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class PaymentRequestProducer {

    private static final Logger logger = LoggerFactory.getLogger(PaymentRequestProducer.class);

    private final KafkaTemplate<String, CloudEvent> kafkaTemplate;
    private final String topicName;
    private final ObjectMapper objectMapper;

    public PaymentRequestProducer(KafkaTemplate<String, CloudEvent> kafkaTemplate,
                                  @Value("${payment.kafka.topics.request}") String topicName) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicName = topicName;
        this.objectMapper = new ObjectMapper();
    }

    public void sendPaymentRequest(PaymentDisbursementRequest request) {
        try {
            byte[] requestData = objectMapper.writeValueAsBytes(request);

            CloudEvent cloudEvent = CloudEventBuilder.v1()
                    .withId(UUID.randomUUID().toString())
                    .withSource(URI.create("payment-service"))
                    .withType("com.ruyalabs.payment.disbursement.request")
                    .withDataContentType("application/json")
                    .withTime(OffsetDateTime.now())
                    .withData(requestData)
                    .build();

            String key = request.getDisbursementId().toString();

            // Create headers for the Kafka message
            List<Header> headers = createKafkaHeaders(request);

            // Log all headers before sending
            logAllHeaders(headers);

            // Create ProducerRecord with headers
            ProducerRecord<String, CloudEvent> producerRecord = new ProducerRecord<>(
                topicName, null, key, cloudEvent, headers);

            kafkaTemplate.send(producerRecord)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            logger.info("Payment request sent successfully for disbursementId: {}", 
                                      request.getDisbursementId());
                        } else {
                            logger.error("Failed to send payment request for disbursementId: {}", 
                                       request.getDisbursementId(), ex);
                        }
                    });
        } catch (Exception e) {
            logger.error("Error creating CloudEvent for payment request: {}", 
                       request.getDisbursementId(), e);
            throw new RuntimeException("Failed to send payment request", e);
        }
    }

    private List<Header> createKafkaHeaders(PaymentDisbursementRequest request) {
        List<Header> headers = new ArrayList<>();

        // Add custom headers with payment information
        headers.add(new RecordHeader("disbursement-id", request.getDisbursementId().toString().getBytes()));
        headers.add(new RecordHeader("payment-method", request.getPaymentMethod().toString().getBytes()));
        headers.add(new RecordHeader("currency", request.getAmount().getCurrency().getBytes()));
        headers.add(new RecordHeader("recipient-name", request.getRecipient().getName().getBytes()));
        headers.add(new RecordHeader("source-service", "payment-service".getBytes()));
        headers.add(new RecordHeader("message-timestamp", String.valueOf(System.currentTimeMillis()).getBytes()));

        return headers;
    }

    private void logAllHeaders(List<Header> headers) {
        logger.info("Kafka message headers being sent to topic: {}", topicName);

        for (Header header : headers) {
            String headerValue = header.value() != null ? new String(header.value()) : "null";
            logger.info("Header - Key: {}, Value: {}", header.key(), headerValue);
        }
    }
}
