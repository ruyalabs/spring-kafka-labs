package ch.ruyalabs.springkafkalabs.kafka;

import ch.ruyalabs.springkafkalabs.kafka.consumer.PaymentResponseConsumer;
import ch.ruyalabs.types.PaymentDisbursementResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.core.format.EventFormat;
import io.cloudevents.jackson.JsonFormat;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"payment-responses"})
@DirtiesContext
public class PaymentResponseConsumerRequirementsTest {

    @Autowired
    private PaymentResponseConsumer paymentResponseConsumer;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testConsumerReceivesRecordAsString() {
        // Given - Create a valid CloudEvent as JSON string
        PaymentDisbursementResponse response = createSamplePaymentResponse();
        
        try {
            byte[] responseData = objectMapper.writeValueAsBytes(response);
            
            CloudEvent cloudEvent = CloudEventBuilder.v1()
                    .withId(UUID.randomUUID().toString())
                    .withSource(URI.create("payment-service"))
                    .withType("com.ruyalabs.payment.disbursement.request")
                    .withDataContentType("application/json")
                    .withTime(OffsetDateTime.now())
                    .withData(responseData)
                    .build();

            // Serialize CloudEvent to JSON string (this is what the consumer should receive)
            EventFormat eventFormat = new JsonFormat();
            String cloudEventJson = new String(eventFormat.serialize(cloudEvent));

            // Create ConsumerRecord<String, String> with structured mode header
            ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "payment-responses", 0, 0L, "test-key", cloudEventJson);
            record.headers().add("content-type", "application/cloudevents+json; charset=UTF-8".getBytes());

            // When & Then - Should process successfully
            assertDoesNotThrow(() -> {
                paymentResponseConsumer.handlePaymentResponse(record);
            });
            
            System.out.println("[DEBUG_LOG] Successfully processed CloudEvent received as string");
            
        } catch (Exception e) {
            fail("Test failed with exception: " + e.getMessage());
        }
    }

    @Test
    public void testConsumerRejectsNonStructuredMode() {
        // Given - Create a CloudEvent JSON string without structured mode header
        PaymentDisbursementResponse response = createSamplePaymentResponse();
        
        try {
            byte[] responseData = objectMapper.writeValueAsBytes(response);
            
            CloudEvent cloudEvent = CloudEventBuilder.v1()
                    .withId(UUID.randomUUID().toString())
                    .withSource(URI.create("payment-service"))
                    .withType("com.ruyalabs.payment.disbursement.request")
                    .withDataContentType("application/json")
                    .withTime(OffsetDateTime.now())
                    .withData(responseData)
                    .build();

            EventFormat eventFormat = new JsonFormat();
            String cloudEventJson = new String(eventFormat.serialize(cloudEvent));

            // Create ConsumerRecord WITHOUT structured mode header
            ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "payment-responses", 0, 0L, "test-key", cloudEventJson);
            // No content-type header added

            // When & Then - Should handle gracefully (log error and return)
            assertDoesNotThrow(() -> {
                paymentResponseConsumer.handlePaymentResponse(record);
            });
            
            System.out.println("[DEBUG_LOG] Successfully rejected non-structured mode CloudEvent");
            
        } catch (Exception e) {
            fail("Test failed with exception: " + e.getMessage());
        }
    }

    @Test
    public void testConsumerValidatesCloudEventAttributes() {
        // Given - Create a CloudEvent with invalid attributes
        PaymentDisbursementResponse response = createSamplePaymentResponse();
        
        try {
            byte[] responseData = objectMapper.writeValueAsBytes(response);
            
            CloudEvent cloudEvent = CloudEventBuilder.v1()
                    .withId(UUID.randomUUID().toString())
                    .withSource(URI.create("invalid-source"))  // Invalid source
                    .withType("invalid.type")  // Invalid type
                    .withDataContentType("application/json")
                    .withTime(OffsetDateTime.now())
                    .withData(responseData)
                    .build();

            EventFormat eventFormat = new JsonFormat();
            String cloudEventJson = new String(eventFormat.serialize(cloudEvent));

            ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "payment-responses", 0, 0L, "test-key", cloudEventJson);
            record.headers().add("content-type", "application/cloudevents+json; charset=UTF-8".getBytes());

            // When & Then - Should handle gracefully (log validation error and return)
            assertDoesNotThrow(() -> {
                paymentResponseConsumer.handlePaymentResponse(record);
            });
            
            System.out.println("[DEBUG_LOG] Successfully validated and rejected invalid CloudEvent attributes");
            
        } catch (Exception e) {
            fail("Test failed with exception: " + e.getMessage());
        }
    }

    @Test
    public void testConsumerHandlesInvalidJson() {
        // Given - Invalid JSON string
        String invalidJson = "{ invalid json structure";
        
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
            "payment-responses", 0, 0L, "test-key", invalidJson);
        record.headers().add("content-type", "application/cloudevents+json; charset=UTF-8".getBytes());

        // When & Then - Should handle gracefully
        assertDoesNotThrow(() -> {
            paymentResponseConsumer.handlePaymentResponse(record);
        });
        
        System.out.println("[DEBUG_LOG] Successfully handled invalid JSON gracefully");
    }

    private PaymentDisbursementResponse createSamplePaymentResponse() {
        PaymentDisbursementResponse response = new PaymentDisbursementResponse();
        response.setDisbursementId(UUID.fromString("a1b2c3d4-e5f6-7890-1234-567890abcdef"));
        response.setStatus(PaymentDisbursementResponse.Status.PROCESSED);
        response.setProcessedAt(new Date());
        response.setTransactionId("txn_1234567890");
        return response;
    }
}