package ch.ruyalabs.springkafkalabs.consumer;

import ch.ruyalabs.springkafkalabs.dto.ErrorDataDto;
import ch.ruyalabs.springkafkalabs.dto.PaymentRequestDto;
import ch.ruyalabs.springkafkalabs.dto.PaymentResponseDto;
import ch.ruyalabs.springkafkalabs.exception.FaultyRequestException;
import ch.ruyalabs.springkafkalabs.exception.InvalidAmountException;
import ch.ruyalabs.springkafkalabs.exception.InvalidCurrencyException;
import ch.ruyalabs.springkafkalabs.exception.MissingPaymentIdException;
import ch.ruyalabs.springkafkalabs.exception.PaymentValidationException;
import ch.ruyalabs.springkafkalabs.service.MailService;
import ch.ruyalabs.springkafkalabs.service.ReliableResponseProducer;
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
    private final ReliableResponseProducer reliableResponseProducer;

    @Value("${app.kafka.topics.payment-execution-request}")
    private String paymentExecutionRequestTopic;

    @Value("${app.kafka.topics.payment-response}")
    private String paymentResponseTopic;

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
            validatePaymentRequest(paymentRequest);

            kafkaTemplate.send(paymentExecutionRequestTopic, paymentRequest.getPaymentId(), paymentRequest);
            log.info("Successfully forwarded payment request to execution topic: paymentId={}", 
                    paymentRequest.getPaymentId());

            acknowledgment.acknowledge();

        } catch (Exception exception) {
            log.error("Error processing payment request: paymentId={}, error={}", 
                    paymentRequest.getPaymentId(), exception.getMessage(), exception);

            handlePaymentError(paymentRequest.getPaymentId(), exception, acknowledgment);
        }
    }

    /**
     * Performs internal validation on the payment request.
     * Throws specific exceptions for various validation failures.
     */
    private void validatePaymentRequest(PaymentRequestDto paymentRequest) throws PaymentValidationException {
        if (Boolean.TRUE.equals(paymentRequest.getIsFaulty())) {
            throw new FaultyRequestException("Payment request is marked as faulty for testing purposes");
        }

        if (!SUPPORTED_CURRENCIES.contains(paymentRequest.getCurrency())) {
            throw new InvalidCurrencyException("Unsupported currency: " + paymentRequest.getCurrency());
        }

        if (paymentRequest.getAmount() == null || paymentRequest.getAmount() <= 0) {
            throw new InvalidAmountException("Invalid payment amount: " + paymentRequest.getAmount());
        }

        if (paymentRequest.getPaymentId() == null || paymentRequest.getPaymentId().trim().isEmpty()) {
            throw new MissingPaymentIdException("Payment ID cannot be null or empty");
        }

        log.debug("Payment request validation passed: paymentId={}", paymentRequest.getPaymentId());
    }

    /**
     * Handles payment processing errors by creating an error response and sending it to the payment-response topic.
     * If sending the error response fails, triggers the ultimate fallback mechanism.
     */
    private void handlePaymentError(String paymentId, Exception exception, Acknowledgment acknowledgment) {
        PaymentResponseDto errorResponse = createErrorResponse(paymentId, exception);

        reliableResponseProducer.sendResponse(paymentResponseTopic, paymentId, errorResponse);

        acknowledgment.acknowledge();
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
        response.setErrorData(errorData);

        return response;
    }

    /**
     * Determines the appropriate error code based on the specific exception type.
     * Uses instanceof checks for reliable exception type detection.
     */
    private String determineErrorCode(Exception exception) {
        if (exception instanceof FaultyRequestException) {
            return "FAULTY_REQUEST_ERROR";
        } else if (exception instanceof InvalidCurrencyException) {
            return "INVALID_CURRENCY_ERROR";
        } else if (exception instanceof InvalidAmountException) {
            return "INVALID_AMOUNT_ERROR";
        } else if (exception instanceof MissingPaymentIdException) {
            return "MISSING_PAYMENT_ID_ERROR";
        } else if (exception instanceof PaymentValidationException) {
            return "VALIDATION_ERROR";
        } else {
            return "INTERNAL_PROCESSING_ERROR";
        }
    }

}
