package ch.ruyalabs.springkafkalabs.consumer;

import ch.ruyalabs.springkafkalabs.dto.ErrorDataDto;
import ch.ruyalabs.springkafkalabs.dto.PaymentRequestDto;
import ch.ruyalabs.springkafkalabs.dto.PaymentResponseDto;
import ch.ruyalabs.springkafkalabs.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentRequestConsumer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final MailService mailService;

    @Value("${app.kafka.topics.payment-execution-request}")
    private String paymentExecutionRequestTopic;

    @Value("${app.kafka.topics.payment-response}")
    private String paymentResponseTopic;

    // List of supported currencies for validation
    private static final List<String> SUPPORTED_CURRENCIES = Arrays.asList("USD", "EUR", "GBP", "CHF");

    @KafkaListener(
        topics = "${app.kafka.topics.payment-request}",
        containerFactory = "paymentRequestKafkaListenerContainerFactory"
    )
    public void consume(
        @Payload PaymentRequestDto paymentRequest,
        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
        @Header(KafkaHeaders.OFFSET) long offset,
        Acknowledgment acknowledgment
    ) {
        log.info("Received payment request: paymentId={}, topic={}, partition={}, offset={}", 
                paymentRequest.getPaymentId(), topic, partition, offset);

        try {
            // Perform internal validation
            validatePaymentRequest(paymentRequest);

            // If validation passes, forward to payment execution topic
            kafkaTemplate.send(paymentExecutionRequestTopic, paymentRequest.getPaymentId(), paymentRequest);
            log.info("Successfully forwarded payment request to execution topic: paymentId={}", 
                    paymentRequest.getPaymentId());

            // Acknowledge the message only after successful processing
            acknowledgment.acknowledge();

        } catch (Exception exception) {
            log.error("Error processing payment request: paymentId={}, error={}", 
                    paymentRequest.getPaymentId(), exception.getMessage(), exception);

            // Handle the error by creating and sending error response
            handlePaymentError(paymentRequest.getPaymentId(), exception, acknowledgment);
        }
    }

    /**
     * Performs internal validation on the payment request.
     * Throws exceptions for various validation failures.
     */
    private void validatePaymentRequest(PaymentRequestDto paymentRequest) {
        // Check if the request is marked as faulty (for testing purposes)
        if (Boolean.TRUE.equals(paymentRequest.getIsFaulty())) {
            throw new IllegalArgumentException("Payment request is marked as faulty for testing purposes");
        }

        // Validate currency
        if (!SUPPORTED_CURRENCIES.contains(paymentRequest.getCurrency())) {
            throw new IllegalArgumentException("Unsupported currency: " + paymentRequest.getCurrency());
        }

        // Validate amount
        if (paymentRequest.getAmount() == null || paymentRequest.getAmount() <= 0) {
            throw new IllegalArgumentException("Invalid payment amount: " + paymentRequest.getAmount());
        }

        // Validate payment ID
        if (paymentRequest.getPaymentId() == null || paymentRequest.getPaymentId().trim().isEmpty()) {
            throw new IllegalArgumentException("Payment ID cannot be null or empty");
        }

        log.debug("Payment request validation passed: paymentId={}", paymentRequest.getPaymentId());
    }

    /**
     * Handles payment processing errors by creating an error response and sending it to the payment-response topic.
     * If sending the error response fails, triggers the ultimate fallback mechanism.
     */
    private void handlePaymentError(String paymentId, Exception exception, Acknowledgment acknowledgment) {
        try {
            // Create error response
            PaymentResponseDto errorResponse = createErrorResponse(paymentId, exception);

            // Send error response to payment-response topic
            kafkaTemplate.send(paymentResponseTopic, paymentId, errorResponse);
            log.info("Successfully sent error response to payment-response topic: paymentId={}", paymentId);

            // Acknowledge the original message after successful error handling
            acknowledgment.acknowledge();

        } catch (Exception fallbackException) {
            log.error("Failed to send error response to payment-response topic: paymentId={}, fallbackError={}", 
                    paymentId, fallbackException.getMessage(), fallbackException);

            // Ultimate fallback: notify operations team
            triggerUltimateFallback(paymentId, exception, fallbackException);

            // Still acknowledge the message to prevent infinite reprocessing
            acknowledgment.acknowledge();
        }
    }

    /**
     * Creates an error response DTO with detailed error information.
     */
    private PaymentResponseDto createErrorResponse(String paymentId, Exception exception) {
        ErrorDataDto errorData = new ErrorDataDto();
        errorData.setErrorCode(determineErrorCode(exception));
        errorData.setErrorMessage(exception.getMessage());
        errorData.setErrorDetails(String.format("Exception type: %s, Stack trace: %s", 
                exception.getClass().getSimpleName(), 
                exception.getStackTrace().length > 0 ? exception.getStackTrace()[0].toString() : "No stack trace"));
        errorData.setOccurredAt(Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant()));

        PaymentResponseDto response = new PaymentResponseDto();
        response.setPaymentId(paymentId);
        response.setAdditionalProperty("errorData", errorData);

        return response;
    }

    /**
     * Determines the appropriate error code based on the exception type.
     */
    private String determineErrorCode(Exception exception) {
        if (exception instanceof IllegalArgumentException) {
            return "VALIDATION_ERROR";
        } else if (exception.getMessage() != null && exception.getMessage().contains("faulty")) {
            return "FAULTY_REQUEST_ERROR";
        } else {
            return "INTERNAL_PROCESSING_ERROR";
        }
    }

    /**
     * Triggers the ultimate fallback mechanism when all other error handling fails.
     */
    private void triggerUltimateFallback(String paymentId, Exception originalException, Exception fallbackException) {
        String errorMessage = String.format(
            "Failed to process payment request and unable to send error response. " +
            "Original error: %s, Fallback error: %s",
            originalException.getMessage(),
            fallbackException.getMessage()
        );

        mailService.sendOperationsNotification(paymentId, errorMessage, originalException);
    }
}
