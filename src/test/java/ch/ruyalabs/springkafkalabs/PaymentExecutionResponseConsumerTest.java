package ch.ruyalabs.springkafkalabs;

import ch.ruyalabs.springkafkalabs.consumer.PaymentExecutionResponseConsumer;
import ch.ruyalabs.springkafkalabs.dto.ErrorDataDto;
import ch.ruyalabs.springkafkalabs.dto.PaymentResponseDto;
import ch.ruyalabs.springkafkalabs.dto.SuccessDataDto;
import ch.ruyalabs.springkafkalabs.service.ReliableResponseProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
    private ReliableResponseProducer reliableResponseProducer;

    @Mock
    private Acknowledgment acknowledgment;

    private PaymentExecutionResponseConsumer paymentExecutionResponseConsumer;

    @BeforeEach
    void setUp() {
        paymentExecutionResponseConsumer = new PaymentExecutionResponseConsumer(reliableResponseProducer);

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
        verify(reliableResponseProducer).sendResponse(eq("payment-response"), eq("test-payment-001"), eq(successResponse));
        verify(acknowledgment).acknowledge();
    }

    @Test
    void testErrorResponseForwarding() {
        // Given
        PaymentResponseDto errorResponse = createErrorResponse("test-payment-error");

        // When
        paymentExecutionResponseConsumer.consume(errorResponse, "payment-execution-response", 0, 124L, acknowledgment);

        // Then
        verify(reliableResponseProducer).sendResponse(eq("payment-response"), eq("test-payment-error"), eq(errorResponse));
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
        response.setSuccessData(successData);

        return response;
    }

    private PaymentResponseDto createErrorResponse(String paymentId) {
        ErrorDataDto errorData = new ErrorDataDto();
        errorData.setErrorCode("TEST_ERROR");
        errorData.setErrorMessage("Some error occurred");
        errorData.setOccurredAt(Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant()));

        PaymentResponseDto response = new PaymentResponseDto();
        response.setPaymentId(paymentId);
        response.setErrorData(errorData);

        return response;
    }
}
