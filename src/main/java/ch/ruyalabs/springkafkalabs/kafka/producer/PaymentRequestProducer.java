package ch.ruyalabs.springkafkalabs.kafka.producer;

import ch.ruyalabs.types.PaymentDisbursementRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.OffsetDateTime;
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

            kafkaTemplate.send(topicName, key, cloudEvent)
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

}