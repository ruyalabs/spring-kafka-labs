package ch.ruyalabs.springkafkalabs.consumer;

import ch.ruyalabs.springkafkalabs.dto.PaymentRequestDto;
import ch.ruyalabs.springkafkalabs.dto.PaymentResponseDto;
import ch.ruyalabs.springkafkalabs.service.MailService;
import ch.ruyalabs.springkafkalabs.service.ReliableResponseProducer;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class PaymentRequestConsumerIntegrationTest {

    @Autowired
    private Validator validator;

    @MockitoBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @MockitoBean
    private MailService mailService;

    @MockitoBean
    private ReliableResponseProducer reliableResponseProducer;

    @MockitoBean
    private Acknowledgment acknowledgment;

    private PaymentRequestConsumer paymentRequestConsumer;

    @BeforeEach
    void setUp() {
        paymentRequestConsumer = new PaymentRequestConsumer(
            kafkaTemplate, 
            mailService, 
            reliableResponseProducer, 
            validator
        );

        // Set the topic values using reflection
        ReflectionTestUtils.setField(paymentRequestConsumer, "paymentExecutionRequestTopic", "payment-execution-request");
        ReflectionTestUtils.setField(paymentRequestConsumer, "paymentResponseTopic", "payment-response");
    }

    @Test
    void testRealValidationWithNullPaymentId() {
        System.out.println("[DEBUG_LOG] Testing real validation with null paymentId");

        // Given - PaymentRequestDto with null paymentId (violates @NotNull)
        PaymentRequestDto invalidRequest = new PaymentRequestDto(null, false, 100.0, "USD");

        // When
        paymentRequestConsumer.consume(invalidRequest, "test-topic", 0, 100L, acknowledgment);

        // Then - Should send error response due to validation failure
        verify(reliableResponseProducer).sendResponse(eq("payment-response"), isNull(), any(PaymentResponseDto.class));
        verify(acknowledgment).acknowledge();
        verifyNoInteractions(kafkaTemplate);

        System.out.println("[DEBUG_LOG] Validation correctly caught null paymentId and sent error response");
    }

    @Test
    void testRealValidationWithNullAmount() {
        System.out.println("[DEBUG_LOG] Testing real validation with null amount");

        // Given - PaymentRequestDto with null amount (violates @NotNull)
        PaymentRequestDto invalidRequest = new PaymentRequestDto("PAY123", false, null, "USD");

        // When
        paymentRequestConsumer.consume(invalidRequest, "test-topic", 0, 100L, acknowledgment);

        // Then - Should send error response due to validation failure
        verify(reliableResponseProducer).sendResponse(eq("payment-response"), eq("PAY123"), any(PaymentResponseDto.class));
        verify(acknowledgment).acknowledge();
        verifyNoInteractions(kafkaTemplate);

        System.out.println("[DEBUG_LOG] Validation correctly caught null amount and sent error response");
    }

    @Test
    void testRealValidationWithInvalidCurrency() {
        System.out.println("[DEBUG_LOG] Testing real validation with invalid currency pattern");

        // Given - PaymentRequestDto with invalid currency (violates @Pattern)
        PaymentRequestDto invalidRequest = new PaymentRequestDto("PAY123", false, 100.0, "INVALID_CURRENCY");

        // When
        paymentRequestConsumer.consume(invalidRequest, "test-topic", 0, 100L, acknowledgment);

        // Then - Should send error response due to validation failure
        verify(reliableResponseProducer).sendResponse(eq("payment-response"), eq("PAY123"), any(PaymentResponseDto.class));
        verify(acknowledgment).acknowledge();
        verifyNoInteractions(kafkaTemplate);

        System.out.println("[DEBUG_LOG] Validation correctly caught invalid currency pattern and sent error response");
    }

    @Test
    void testRealValidationWithValidRequest() {
        System.out.println("[DEBUG_LOG] Testing real validation with valid request");

        // Given - Valid PaymentRequestDto
        PaymentRequestDto validRequest = new PaymentRequestDto("PAY123", false, 100.0, "USD");

        // When
        paymentRequestConsumer.consume(validRequest, "test-topic", 0, 100L, acknowledgment);

        // Then - Should process normally and forward to execution topic
        verify(kafkaTemplate).send("payment-execution-request", "PAY123", validRequest);
        verify(acknowledgment).acknowledge();
        verifyNoInteractions(reliableResponseProducer);

        System.out.println("[DEBUG_LOG] Valid request processed successfully and forwarded to execution topic");
    }

    @Test
    void testRealValidationWithMultipleErrors() {
        System.out.println("[DEBUG_LOG] Testing real validation with multiple validation errors");

        // Given - PaymentRequestDto with multiple validation errors
        PaymentRequestDto invalidRequest = new PaymentRequestDto(null, null, null, "INVALID");

        // When
        paymentRequestConsumer.consume(invalidRequest, "test-topic", 0, 100L, acknowledgment);

        // Then - Should send error response due to validation failures
        verify(reliableResponseProducer).sendResponse(eq("payment-response"), isNull(), any(PaymentResponseDto.class));
        verify(acknowledgment).acknowledge();
        verifyNoInteractions(kafkaTemplate);

        System.out.println("[DEBUG_LOG] Validation correctly caught multiple errors and sent error response");
    }
}
