package ch.ruyalabs.springkafkalabs.service;

import ch.ruyalabs.springkafkalabs.dto.PaymentResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReliableResponseProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final MailService mailService;

    /**
     * Reliably sends a PaymentResponseDto to the specified topic.
     * If the send operation fails, triggers the ultimate fallback mechanism.
     * 
     * @param topic The Kafka topic to send the response to
     * @param paymentId The payment ID for correlation
     * @param response The PaymentResponseDto to send
     */
    public void sendResponse(String topic, String paymentId, PaymentResponseDto response) {
        try {
            kafkaTemplate.send(topic, paymentId, response);
            log.info("Successfully sent payment response to topic: paymentId={}, topic={}", paymentId, topic);
            
        } catch (Exception exception) {
            log.error("Failed to send payment response to topic: paymentId={}, topic={}, error={}", 
                    paymentId, topic, exception.getMessage(), exception);
            
            // Trigger ultimate fallback mechanism
            String errorMessage = String.format(
                "Failed to send payment response to topic '%s'. Error: %s",
                topic,
                exception.getMessage()
            );
            
            mailService.sendOperationsNotification(paymentId, errorMessage, exception);
        }
    }
}