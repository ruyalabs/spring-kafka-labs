package ch.ruyalabs.springkafkalabs;

import ch.ruyalabs.springkafkalabs.consumer.PaymentExecutionResponseConsumer;
import ch.ruyalabs.springkafkalabs.dto.PaymentResponseDto;
import ch.ruyalabs.springkafkalabs.dto.SuccessDataDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentExecutionResponseConsumerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private Acknowledgment acknowledgment;

    private PaymentExecutionResponseConsumer paymentExecutionResponseConsumer;

    @BeforeEach
    void setUp() {
        paymentExecutionResponseConsumer = new PaymentExecutionResponseConsumer(kafkaTemplate);
        
        // Set the topic name using reflection since it's @Value injected
        ReflectionTestUtils.setField(paymentExecutionResponseConsumer, "paymentResponseTopic", "payment-response");
    }

    @Test
    void testSuccessfulResponseForwarding() {
        // Given
        PaymentResponseDto successResponse = createSuccessResponse("test-payment-001");

        // When
        paymentExecutionResponseConsumer.consume(successResponse, "payment-execution-response", 0, 123L, acknowledgment);

        // Then
        verify(kafkaTemplate).send(eq("payment-response"), eq("test-payment-001"), eq(successResponse));
        verify(acknowledgment).acknowledge();
    }

    @Test
    void testErrorResponseForwarding() {
        // Given
        PaymentResponseDto errorResponse = createErrorResponse("test-payment-error");

        // When
        paymentExecutionResponseConsumer.consume(errorResponse, "payment-execution-response", 0, 124L, acknowledgment);

        // Then
        verify(kafkaTemplate).send(eq("payment-response"), eq("test-payment-error"), eq(errorResponse));
        verify(acknowledgment).acknowledge();
    }

    @Test
    void testKafkaProducerFailureHandling() {
        // Given
        PaymentResponseDto response = createSuccessResponse("test-payment-failure");
        doThrow(new RuntimeException("Kafka producer failure")).when(kafkaTemplate).send(anyString(), anyString(), any());

        // When
        paymentExecutionResponseConsumer.consume(response, "payment-execution-response", 0, 125L, acknowledgment);

        // Then
        // Even if forwarding fails, the message should still be acknowledged to prevent infinite reprocessing
        verify(acknowledgment).acknowledge();
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

    private PaymentResponseDto createErrorResponse(String paymentId) {
        PaymentResponseDto response = new PaymentResponseDto();
        response.setPaymentId(paymentId);
        response.setAdditionalProperty("errorData", "Some error occurred");

        return response;
    }
}