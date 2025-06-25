package ch.ruyalabs.springkafkalabs;

import ch.ruyalabs.springkafkalabs.consumer.PaymentRequestConsumer;
import ch.ruyalabs.springkafkalabs.dto.PaymentRequestDto;
import ch.ruyalabs.springkafkalabs.service.MailService;
import ch.ruyalabs.springkafkalabs.service.ReliableResponseProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentRequestConsumerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private MailService mailService;

    @Mock
    private ReliableResponseProducer reliableResponseProducer;

    @Mock
    private Acknowledgment acknowledgment;

    private PaymentRequestConsumer paymentRequestConsumer;

    @BeforeEach
    void setUp() {
        paymentRequestConsumer = new PaymentRequestConsumer(kafkaTemplate, mailService, reliableResponseProducer);

        // Set the topic names using reflection since they're @Value injected
        ReflectionTestUtils.setField(paymentRequestConsumer, "paymentExecutionRequestTopic", "payment-execution-request");
        ReflectionTestUtils.setField(paymentRequestConsumer, "paymentResponseTopic", "payment-response");
    }

    @Test
    void testValidPaymentRequestProcessing() {
        // Given
        PaymentRequestDto validRequest = createValidPaymentRequest();

        // When
        paymentRequestConsumer.consume(validRequest, "payment-request", 0, 123L, acknowledgment);

        // Then
        verify(kafkaTemplate).send(eq("payment-execution-request"), eq("test-payment-001"), eq(validRequest));
        verify(acknowledgment).acknowledge();
        verifyNoInteractions(mailService);
    }

    @Test
    void testFaultyPaymentRequestHandling() {
        // Given
        PaymentRequestDto faultyRequest = createFaultyPaymentRequest();

        // When
        paymentRequestConsumer.consume(faultyRequest, "payment-request", 0, 123L, acknowledgment);

        // Then
        verify(reliableResponseProducer).sendResponse(eq("payment-response"), eq("test-payment-faulty"), any());
        verify(acknowledgment).acknowledge();
        verifyNoInteractions(kafkaTemplate); // Error handling should not use kafkaTemplate directly
    }

    @Test
    void testInvalidCurrencyHandling() {
        // Given
        PaymentRequestDto invalidRequest = createInvalidCurrencyRequest();

        // When
        paymentRequestConsumer.consume(invalidRequest, "payment-request", 0, 123L, acknowledgment);

        // Then
        verify(reliableResponseProducer).sendResponse(eq("payment-response"), eq("test-payment-invalid"), any());
        verify(acknowledgment).acknowledge();
        verifyNoInteractions(kafkaTemplate); // Error handling should not use kafkaTemplate directly
    }


    private PaymentRequestDto createValidPaymentRequest() {
        PaymentRequestDto request = new PaymentRequestDto();
        request.setPaymentId("test-payment-001");
        request.setIsFaulty(false);
        request.setAmount(100.50);
        request.setCurrency("USD");
        request.setDescription("Valid test payment");
        return request;
    }

    private PaymentRequestDto createFaultyPaymentRequest() {
        PaymentRequestDto request = new PaymentRequestDto();
        request.setPaymentId("test-payment-faulty");
        request.setIsFaulty(true); // This should trigger an error
        request.setAmount(50.0);
        request.setCurrency("USD");
        request.setDescription("Faulty test payment");
        return request;
    }

    private PaymentRequestDto createInvalidCurrencyRequest() {
        PaymentRequestDto request = new PaymentRequestDto();
        request.setPaymentId("test-payment-invalid");
        request.setIsFaulty(false);
        request.setAmount(75.0);
        request.setCurrency("XYZ"); // Invalid currency
        request.setDescription("Invalid currency test payment");
        return request;
    }
}
