package ch.ruyalabs.springkafkalabs;

import ch.ruyalabs.springkafkalabs.dto.ErrorDataDto;
import ch.ruyalabs.springkafkalabs.dto.PaymentResponseDto;
import ch.ruyalabs.springkafkalabs.dto.SuccessDataDto;
import ch.ruyalabs.springkafkalabs.service.MailService;
import ch.ruyalabs.springkafkalabs.service.ReliableResponseProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReliableResponseProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private MailService mailService;

    private ReliableResponseProducer reliableResponseProducer;

    @BeforeEach
    void setUp() {
        reliableResponseProducer = new ReliableResponseProducer(kafkaTemplate, mailService);
    }

    @Test
    void testSuccessfulResponseSending() {
        // Given
        PaymentResponseDto response = createSuccessResponse("test-payment-001");
        String topic = "payment-response";
        String paymentId = "test-payment-001";

        // When
        reliableResponseProducer.sendResponse(topic, paymentId, response);

        // Then
        verify(kafkaTemplate).send(eq(topic), eq(paymentId), eq(response));
        verifyNoInteractions(mailService);
    }

    @Test
    void testErrorResponseSending() {
        // Given
        PaymentResponseDto response = createErrorResponse("test-payment-error");
        String topic = "payment-response";
        String paymentId = "test-payment-error";

        // When
        reliableResponseProducer.sendResponse(topic, paymentId, response);

        // Then
        verify(kafkaTemplate).send(eq(topic), eq(paymentId), eq(response));
        verifyNoInteractions(mailService);
    }

    @Test
    void testKafkaFailureTriggersFallback() {
        // Given
        PaymentResponseDto response = createSuccessResponse("test-payment-failure");
        String topic = "payment-response";
        String paymentId = "test-payment-failure";
        RuntimeException kafkaException = new RuntimeException("Kafka producer failure");
        
        doThrow(kafkaException).when(kafkaTemplate).send(anyString(), anyString(), any());

        // When
        reliableResponseProducer.sendResponse(topic, paymentId, response);

        // Then
        verify(kafkaTemplate).send(eq(topic), eq(paymentId), eq(response));
        verify(mailService).sendOperationsNotification(
            eq(paymentId), 
            contains("Failed to send payment response to topic 'payment-response'"), 
            eq(kafkaException)
        );
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