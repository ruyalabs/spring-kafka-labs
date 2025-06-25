package ch.ruyalabs.springkafkalabs;

import ch.ruyalabs.springkafkalabs.dto.ErrorDataDto;
import ch.ruyalabs.springkafkalabs.dto.PaymentRequestDto;
import ch.ruyalabs.springkafkalabs.dto.PaymentResponseDto;
import ch.ruyalabs.springkafkalabs.dto.SuccessDataDto;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DirtiesContext
@EmbeddedKafka(
    partitions = 1,
    topics = {
        "payment-request",
        "payment-response", 
        "payment-execution-request",
        "payment-execution-response"
    }
)
public class PaymentProcessingIntegrationTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Test
    public void testHappyPathPaymentProcessing() throws Exception {
        // Setup consumers to capture messages
        BlockingQueue<ConsumerRecord<String, PaymentRequestDto>> executionRequestRecords = new LinkedBlockingQueue<>();
        BlockingQueue<ConsumerRecord<String, PaymentResponseDto>> finalResponseRecords = new LinkedBlockingQueue<>();

        // Create consumer for payment-execution-request topic
        KafkaMessageListenerContainer<String, PaymentRequestDto> executionRequestContainer = 
            createPaymentRequestConsumer("payment-execution-request", executionRequestRecords);
        executionRequestContainer.start();

        // Create consumer for payment-response topic
        KafkaMessageListenerContainer<String, PaymentResponseDto> finalResponseContainer = 
            createPaymentResponseConsumer("payment-response", finalResponseRecords);
        finalResponseContainer.start();

        // Create producer for sending messages
        KafkaTemplate<String, Object> producer = createProducer();

        try {
            // Step 1: Send a valid payment request
            PaymentRequestDto validRequest = new PaymentRequestDto();
            validRequest.setPaymentId("test-payment-001");
            validRequest.setIsFaulty(false);
            validRequest.setAmount(100.50);
            validRequest.setCurrency("USD");
            validRequest.setDescription("Test payment");

            producer.send("payment-request", validRequest.getPaymentId(), validRequest);

            // Step 2: Verify the request was forwarded to payment-execution-request
            ConsumerRecord<String, PaymentRequestDto> executionRecord = 
                executionRequestRecords.poll(10, TimeUnit.SECONDS);
            assertNotNull(executionRecord, "Payment request should be forwarded to execution topic");
            assertEquals("test-payment-001", executionRecord.key());
            assertEquals("test-payment-001", executionRecord.value().getPaymentId());
            assertEquals(100.50, executionRecord.value().getAmount());

            // Step 3: Simulate external system response (success)
            PaymentResponseDto successResponse = createSuccessResponse("test-payment-001");
            producer.send("payment-execution-response", successResponse.getPaymentId(), successResponse);

            // Step 4: Verify the success response was forwarded to final topic
            ConsumerRecord<String, PaymentResponseDto> finalRecord = 
                finalResponseRecords.poll(10, TimeUnit.SECONDS);
            assertNotNull(finalRecord, "Success response should be forwarded to final topic");
            assertEquals("test-payment-001", finalRecord.key());
            assertEquals("test-payment-001", finalRecord.value().getPaymentId());

        } finally {
            executionRequestContainer.stop();
            finalResponseContainer.stop();
        }
    }

    @Test
    public void testErrorHandlingForFaultyRequest() throws Exception {
        // Setup consumer to capture error responses
        BlockingQueue<ConsumerRecord<String, PaymentResponseDto>> errorResponseRecords = new LinkedBlockingQueue<>();

        KafkaMessageListenerContainer<String, PaymentResponseDto> errorResponseContainer = 
            createPaymentResponseConsumer("payment-response", errorResponseRecords);
        errorResponseContainer.start();

        KafkaTemplate<String, Object> producer = createProducer();

        try {
            // Send a faulty payment request
            PaymentRequestDto faultyRequest = new PaymentRequestDto();
            faultyRequest.setPaymentId("test-payment-error-001");
            faultyRequest.setIsFaulty(true); // This should trigger an error
            faultyRequest.setAmount(50.0);
            faultyRequest.setCurrency("USD");

            producer.send("payment-request", faultyRequest.getPaymentId(), faultyRequest);

            // Verify error response is sent to payment-response topic
            ConsumerRecord<String, PaymentResponseDto> errorRecord = 
                errorResponseRecords.poll(10, TimeUnit.SECONDS);
            assertNotNull(errorRecord, "Error response should be sent to final topic");
            assertEquals("test-payment-error-001", errorRecord.key());
            assertEquals("test-payment-error-001", errorRecord.value().getPaymentId());

            // Verify it contains error data
            Object errorData = errorRecord.value().getAdditionalProperties().get("errorData");
            assertNotNull(errorData, "Response should contain error data");

        } finally {
            errorResponseContainer.stop();
        }
    }

    @Test
    public void testValidationErrorHandling() throws Exception {
        // Setup consumer to capture error responses
        BlockingQueue<ConsumerRecord<String, PaymentResponseDto>> errorResponseRecords = new LinkedBlockingQueue<>();

        KafkaMessageListenerContainer<String, PaymentResponseDto> errorResponseContainer = 
            createPaymentResponseConsumer("payment-response", errorResponseRecords);
        errorResponseContainer.start();

        KafkaTemplate<String, Object> producer = createProducer();

        try {
            // Send a payment request with invalid currency
            PaymentRequestDto invalidRequest = new PaymentRequestDto();
            invalidRequest.setPaymentId("test-payment-validation-001");
            invalidRequest.setIsFaulty(false);
            invalidRequest.setAmount(75.0);
            invalidRequest.setCurrency("XYZ"); // Invalid currency

            producer.send("payment-request", invalidRequest.getPaymentId(), invalidRequest);

            // Verify error response is sent
            ConsumerRecord<String, PaymentResponseDto> errorRecord = 
                errorResponseRecords.poll(10, TimeUnit.SECONDS);
            assertNotNull(errorRecord, "Validation error response should be sent");
            assertEquals("test-payment-validation-001", errorRecord.key());

            // Verify it contains validation error data
            Object errorData = errorRecord.value().getAdditionalProperties().get("errorData");
            assertNotNull(errorData, "Response should contain validation error data");

        } finally {
            errorResponseContainer.stop();
        }
    }

    private PaymentResponseDto createSuccessResponse(String paymentId) {
        SuccessDataDto successData = new SuccessDataDto();
        successData.setTransactionId("txn-" + paymentId);
        successData.setProcessedAmount(100.50);
        successData.setProcessedAt(Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant()));
        successData.setStatus(SuccessDataDto.Status.COMPLETED);

        PaymentResponseDto response = new PaymentResponseDto();
        response.setPaymentId(paymentId);
        response.setAdditionalProperty("successData", successData);

        return response;
    }

    private KafkaTemplate<String, Object> createProducer() {
        Map<String, Object> producerProps = KafkaTestUtils.producerProps(embeddedKafkaBroker);
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        DefaultKafkaProducerFactory<String, Object> producerFactory = 
            new DefaultKafkaProducerFactory<>(producerProps);

        return new KafkaTemplate<>(producerFactory);
    }

    private KafkaMessageListenerContainer<String, PaymentRequestDto> createPaymentRequestConsumer(
            String topic, BlockingQueue<ConsumerRecord<String, PaymentRequestDto>> records) {

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("test-group", "true", embeddedKafkaBroker);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "ch.ruyalabs.springkafkalabs.dto");
        consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, PaymentRequestDto.class.getName());

        DefaultKafkaConsumerFactory<String, PaymentRequestDto> consumerFactory = 
            new DefaultKafkaConsumerFactory<>(consumerProps);

        ContainerProperties containerProperties = new ContainerProperties(topic);
        containerProperties.setMessageListener((MessageListener<String, PaymentRequestDto>) records::add);

        return new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
    }

    private KafkaMessageListenerContainer<String, PaymentResponseDto> createPaymentResponseConsumer(
            String topic, BlockingQueue<ConsumerRecord<String, PaymentResponseDto>> records) {

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("test-group-response", "true", embeddedKafkaBroker);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "ch.ruyalabs.springkafkalabs.dto");
        consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, PaymentResponseDto.class.getName());

        DefaultKafkaConsumerFactory<String, PaymentResponseDto> consumerFactory = 
            new DefaultKafkaConsumerFactory<>(consumerProps);

        ContainerProperties containerProperties = new ContainerProperties(topic);
        containerProperties.setMessageListener((MessageListener<String, PaymentResponseDto>) records::add);

        return new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
    }
}
