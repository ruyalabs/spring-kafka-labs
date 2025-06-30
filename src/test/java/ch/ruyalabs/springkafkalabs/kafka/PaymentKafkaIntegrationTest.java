package ch.ruyalabs.springkafkalabs.kafka;

import ch.ruyalabs.springkafkalabs.kafka.consumer.PaymentResponseConsumer;
import ch.ruyalabs.springkafkalabs.kafka.producer.PaymentRequestProducer;
import ch.ruyalabs.types.*;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeaders;
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
@EmbeddedKafka(partitions = 1, topics = {"payment-requests", "payment-responses"})
@DirtiesContext
public class PaymentKafkaIntegrationTest {

    @Autowired
    private PaymentRequestProducer paymentRequestProducer;

    @Autowired
    private PaymentResponseConsumer paymentResponseConsumer;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testPaymentRequestProducerCreatesValidCloudEvent() {
        // Given
        PaymentDisbursementRequest request = createSamplePaymentRequest();

        // When & Then - Should not throw exception
        assertDoesNotThrow(() -> {
            paymentRequestProducer.sendPaymentRequest(request);
        });
    }

    @Test
    public void testPaymentResponseConsumerCanProcessCloudEvent() throws Exception {
        // Given
        PaymentDisbursementResponse response = createSamplePaymentResponse();
        byte[] responseData = objectMapper.writeValueAsBytes(response);

        CloudEvent cloudEvent = CloudEventBuilder.v1()
                .withId(UUID.randomUUID().toString())
                .withSource(URI.create("payment-processor"))
                .withType("com.ruyalabs.payment.disbursement.response")
                .withDataContentType("application/json")
                .withTime(OffsetDateTime.now())
                .withData(responseData)
                .build();

        // Create a ConsumerRecord to match the new method signature
        ConsumerRecord<String, CloudEvent> consumerRecord = new ConsumerRecord<>(
            "payment-responses", 0, 0L, "test-key", cloudEvent);

        // When & Then - Should not throw exception
        assertDoesNotThrow(() -> {
            paymentResponseConsumer.handlePaymentResponse(consumerRecord);
        });
    }

    @Test
    public void testCloudEventSerialization() throws Exception {
        // Given
        PaymentDisbursementRequest request = createSamplePaymentRequest();
        ObjectMapper mapper = new ObjectMapper();

        // When
        byte[] requestData = mapper.writeValueAsBytes(request);

        CloudEvent cloudEvent = CloudEventBuilder.v1()
                .withId(UUID.randomUUID().toString())
                .withSource(URI.create("payment-service"))
                .withType("com.ruyalabs.payment.disbursement.request")
                .withDataContentType("application/json")
                .withTime(OffsetDateTime.now())
                .withData(requestData)
                .build();

        // Then
        assertNotNull(cloudEvent);
        assertNotNull(cloudEvent.getId());
        assertEquals("payment-service", cloudEvent.getSource().toString());
        assertEquals("com.ruyalabs.payment.disbursement.request", cloudEvent.getType());
        assertEquals("application/json", cloudEvent.getDataContentType());
        assertNotNull(cloudEvent.getData());

        // Verify we can deserialize the data back
        PaymentDisbursementRequest deserializedRequest = mapper.readValue(
            cloudEvent.getData().toBytes(), 
            PaymentDisbursementRequest.class
        );
        assertEquals(request.getDisbursementId(), deserializedRequest.getDisbursementId());
        assertEquals(request.getRecipient().getName(), deserializedRequest.getRecipient().getName());
    }

    private PaymentDisbursementRequest createSamplePaymentRequest() {
        PaymentDisbursementRequest request = new PaymentDisbursementRequest();

        request.setDisbursementId(UUID.fromString("4a7c6a5a-4d72-4c2c-a1d2-7e3a2b1c8d7e"));

        PaymentRecipient recipient = new PaymentRecipient();
        recipient.setName("Jane Doe");
        recipient.setEmail("jane.doe@example.com");

        BankAccountDetails bankDetails = new BankAccountDetails();
        bankDetails.setAccountNumber("1234567890");
        bankDetails.setSortCode("11-22-33");
        bankDetails.setIban("GB29NWBK60161331926819");
        recipient.setBankDetails(bankDetails);

        request.setRecipient(recipient);

        PaymentAmount amount = new PaymentAmount();
        amount.setValue(1500.75);
        amount.setCurrency("USD");
        request.setAmount(amount);

        request.setPaymentMethod(PaymentDisbursementRequest.PaymentMethod.BANK_TRANSFER);
        request.setRequestedAt(new Date());

        PaymentMetadata metadata = new PaymentMetadata();
        metadata.setAdditionalProperty("internalReference", "TXN-12345-ABC");
        metadata.setAdditionalProperty("notes", "Monthly salary payment.");
        request.setMetadata(metadata);

        return request;
    }

    private PaymentDisbursementResponse createSamplePaymentResponse() {
        PaymentDisbursementResponse response = new PaymentDisbursementResponse();

        response.setDisbursementId(UUID.fromString("a1b2c3d4-e5f6-7890-1234-567890abcdef"));
        response.setStatus(PaymentDisbursementResponse.Status.PROCESSED);
        response.setProcessedAt(new Date());
        response.setTransactionId("txn_1234567890");

        DisbursedAmount amount = new DisbursedAmount();
        amount.setValue(150.75);
        amount.setCurrency("USD");
        response.setAmount(amount);

        return response;
    }
}
