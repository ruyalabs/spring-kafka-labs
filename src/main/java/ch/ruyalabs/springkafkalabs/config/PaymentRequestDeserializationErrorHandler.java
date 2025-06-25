package ch.ruyalabs.springkafkalabs.config;

import ch.ruyalabs.springkafkalabs.dto.ErrorDataDto;
import ch.ruyalabs.springkafkalabs.dto.PaymentResponseDto;
import ch.ruyalabs.springkafkalabs.service.ReliableResponseProducer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentRequestDeserializationErrorHandler extends DefaultErrorHandler {

    private final ReliableResponseProducer reliableResponseProducer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.kafka.topics.payment-response}")
    private String paymentResponseTopic;

    @Override
    public void handleRemaining(Exception thrownException, 
                               java.util.List<ConsumerRecord<?, ?>> records, 
                               Consumer<?, ?> consumer, 
                               org.springframework.kafka.listener.MessageListenerContainer container) {
        
        log.error("Handling deserialization error for {} records", records.size(), thrownException);
        
        for (ConsumerRecord<?, ?> record : records) {
            handleSingleRecord(thrownException, record);
        }
        
        // Call parent to handle offset commits
        super.handleRemaining(thrownException, records, consumer, container);
    }

    private void handleSingleRecord(Exception thrownException, ConsumerRecord<?, ?> record) {
        String paymentId = extractPaymentId(record);
        
        log.error("Processing deserialization error for record: topic={}, partition={}, offset={}, paymentId={}", 
                record.topic(), record.partition(), record.offset(), paymentId, thrownException);

        PaymentResponseDto errorResponse = createDeserializationErrorResponse(paymentId, thrownException);
        
        try {
            reliableResponseProducer.sendResponse(paymentResponseTopic, paymentId, errorResponse);
            log.info("Successfully sent deserialization error response: paymentId={}", paymentId);
        } catch (Exception sendException) {
            log.error("Failed to send deserialization error response: paymentId={}", paymentId, sendException);
            // ReliableResponseProducer already handles fallback mechanisms
        }
    }

    private String extractPaymentId(ConsumerRecord<?, ?> record) {
        try {
            // Try to extract paymentId from the raw message value
            Object value = record.value();
            if (value == null) {
                log.warn("Record value is null, using UNKNOWN paymentId");
                return "UNKNOWN";
            }

            String jsonString;
            if (value instanceof byte[]) {
                jsonString = new String((byte[]) value);
            } else if (value instanceof String) {
                jsonString = (String) value;
            } else {
                jsonString = value.toString();
            }

            JsonNode jsonNode = objectMapper.readTree(jsonString);
            JsonNode paymentIdNode = jsonNode.get("paymentId");
            
            if (paymentIdNode != null && !paymentIdNode.isNull()) {
                String paymentId = paymentIdNode.asText();
                log.debug("Successfully extracted paymentId: {}", paymentId);
                return paymentId;
            } else {
                log.warn("PaymentId field not found in message, using UNKNOWN");
                return "UNKNOWN";
            }
            
        } catch (Exception exception) {
            log.warn("Failed to extract paymentId from malformed message: {}", exception.getMessage());
            return "UNKNOWN";
        }
    }

    private PaymentResponseDto createDeserializationErrorResponse(String paymentId, Exception exception) {
        ErrorDataDto errorData = new ErrorDataDto();
        errorData.setErrorCode("DESERIALIZATION_ERROR");
        errorData.setErrorMessage("The payment request could not be parsed due to invalid format or structure");
        errorData.setErrorDetails(String.format("Exception type: %s, Message: %s", 
                exception.getClass().getSimpleName(), 
                exception.getMessage()));
        errorData.setOccurredAt(Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant()));

        PaymentResponseDto response = new PaymentResponseDto();
        response.setPaymentId(paymentId);
        response.setErrorData(errorData);

        return response;
    }
}