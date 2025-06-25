package ch.ruyalabs.springkafkalabs.consumer;

import ch.ruyalabs.springkafkalabs.dto.PaymentRequestDto;
import ch.ruyalabs.springkafkalabs.dto.PaymentResponseDto;
import ch.ruyalabs.springkafkalabs.service.MailService;
import ch.ruyalabs.springkafkalabs.service.ReliableResponseProducer;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentRequestConsumerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    @Mock
    private MailService mailService;
    
    @Mock
    private ReliableResponseProducer reliableResponseProducer;
    
    @Mock
    private Validator validator;
    
    @Mock
    private Acknowledgment acknowledgment;
    
    @Mock
    private ConstraintViolation<PaymentRequestDto> constraintViolation;

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
    void testConsumeWithValidPaymentRequest() {
        // Given
        PaymentRequestDto validRequest = new PaymentRequestDto("PAY123", false, 100.0, "USD");
        when(validator.validate(validRequest)).thenReturn(Set.of());

        // When
        paymentRequestConsumer.consume(validRequest, "test-topic", 0, 100L, acknowledgment);

        // Then
        verify(validator).validate(validRequest);
        verify(kafkaTemplate).send("payment-execution-request", "PAY123", validRequest);
        verify(acknowledgment).acknowledge();
        verifyNoInteractions(reliableResponseProducer);
    }

    @Test
    void testConsumeWithNullPaymentId() {
        // Given
        PaymentRequestDto invalidRequest = new PaymentRequestDto(null, false, 100.0, "USD");
        when(constraintViolation.getPropertyPath()).thenReturn(mock(jakarta.validation.Path.class));
        when(constraintViolation.getPropertyPath().toString()).thenReturn("paymentId");
        when(constraintViolation.getMessage()).thenReturn("must not be null");
        when(validator.validate(invalidRequest)).thenReturn(Set.of(constraintViolation));

        // When
        paymentRequestConsumer.consume(invalidRequest, "test-topic", 0, 100L, acknowledgment);

        // Then
        verify(validator).validate(invalidRequest);
        verify(reliableResponseProducer).sendResponse(eq("payment-response"), isNull(), any(PaymentResponseDto.class));
        verify(acknowledgment).acknowledge();
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void testConsumeWithNullAmount() {
        // Given
        PaymentRequestDto invalidRequest = new PaymentRequestDto("PAY123", false, null, "USD");
        when(constraintViolation.getPropertyPath()).thenReturn(mock(jakarta.validation.Path.class));
        when(constraintViolation.getPropertyPath().toString()).thenReturn("amount");
        when(constraintViolation.getMessage()).thenReturn("must not be null");
        when(validator.validate(invalidRequest)).thenReturn(Set.of(constraintViolation));

        // When
        paymentRequestConsumer.consume(invalidRequest, "test-topic", 0, 100L, acknowledgment);

        // Then
        verify(validator).validate(invalidRequest);
        verify(reliableResponseProducer).sendResponse(eq("payment-response"), eq("PAY123"), any(PaymentResponseDto.class));
        verify(acknowledgment).acknowledge();
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void testConsumeWithInvalidCurrencyPattern() {
        // Given
        PaymentRequestDto invalidRequest = new PaymentRequestDto("PAY123", false, 100.0, "INVALID");
        when(constraintViolation.getPropertyPath()).thenReturn(mock(jakarta.validation.Path.class));
        when(constraintViolation.getPropertyPath().toString()).thenReturn("currency");
        when(constraintViolation.getMessage()).thenReturn("must match \"^[A-Z]{3}$\"");
        when(validator.validate(invalidRequest)).thenReturn(Set.of(constraintViolation));

        // When
        paymentRequestConsumer.consume(invalidRequest, "test-topic", 0, 100L, acknowledgment);

        // Then
        verify(validator).validate(invalidRequest);
        verify(reliableResponseProducer).sendResponse(eq("payment-response"), eq("PAY123"), any(PaymentResponseDto.class));
        verify(acknowledgment).acknowledge();
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void testConsumeWithMultipleValidationErrors() {
        // Given
        PaymentRequestDto invalidRequest = new PaymentRequestDto(null, null, null, "INVALID");
        
        ConstraintViolation<PaymentRequestDto> violation1 = mock(ConstraintViolation.class);
        when(violation1.getPropertyPath()).thenReturn(mock(jakarta.validation.Path.class));
        when(violation1.getPropertyPath().toString()).thenReturn("paymentId");
        when(violation1.getMessage()).thenReturn("must not be null");
        
        ConstraintViolation<PaymentRequestDto> violation2 = mock(ConstraintViolation.class);
        when(violation2.getPropertyPath()).thenReturn(mock(jakarta.validation.Path.class));
        when(violation2.getPropertyPath().toString()).thenReturn("amount");
        when(violation2.getMessage()).thenReturn("must not be null");
        
        when(validator.validate(invalidRequest)).thenReturn(Set.of(violation1, violation2));

        // When
        paymentRequestConsumer.consume(invalidRequest, "test-topic", 0, 100L, acknowledgment);

        // Then
        verify(validator).validate(invalidRequest);
        
        ArgumentCaptor<PaymentResponseDto> responseCaptor = ArgumentCaptor.forClass(PaymentResponseDto.class);
        verify(reliableResponseProducer).sendResponse(eq("payment-response"), isNull(), responseCaptor.capture());
        
        PaymentResponseDto capturedResponse = responseCaptor.getValue();
        assertNotNull(capturedResponse.getErrorData());
        assertEquals("JAKARTA_VALIDATION_ERROR", capturedResponse.getErrorData().getErrorCode());
        assertTrue(capturedResponse.getErrorData().getErrorMessage().contains("Validation failed"));
        
        verify(acknowledgment).acknowledge();
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void testConsumeWithFaultyRequest() {
        // Given
        PaymentRequestDto faultyRequest = new PaymentRequestDto("PAY123", true, 100.0, "USD");
        when(validator.validate(faultyRequest)).thenReturn(Set.of());

        // When
        paymentRequestConsumer.consume(faultyRequest, "test-topic", 0, 100L, acknowledgment);

        // Then
        verify(validator).validate(faultyRequest);
        
        ArgumentCaptor<PaymentResponseDto> responseCaptor = ArgumentCaptor.forClass(PaymentResponseDto.class);
        verify(reliableResponseProducer).sendResponse(eq("payment-response"), eq("PAY123"), responseCaptor.capture());
        
        PaymentResponseDto capturedResponse = responseCaptor.getValue();
        assertNotNull(capturedResponse.getErrorData());
        assertEquals("FAULTY_REQUEST_ERROR", capturedResponse.getErrorData().getErrorCode());
        
        verify(acknowledgment).acknowledge();
        verifyNoInteractions(kafkaTemplate);
    }
}