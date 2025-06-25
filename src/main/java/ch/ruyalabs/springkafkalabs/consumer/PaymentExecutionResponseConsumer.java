package ch.ruyalabs.springkafkalabs.consumer;

import ch.ruyalabs.springkafkalabs.dto.PaymentResponseDto;
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

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentExecutionResponseConsumer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.payment-response}")
    private String paymentResponseTopic;

    @KafkaListener(
        topics = "${app.kafka.topics.payment-execution-response}",
        containerFactory = "paymentResponseKafkaListenerContainerFactory"
    )
    public void consume(
        @Payload PaymentResponseDto paymentResponse,
        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
        @Header(KafkaHeaders.OFFSET) long offset,
        Acknowledgment acknowledgment
    ) {
        log.info("Received payment execution response: paymentId={}, topic={}, partition={}, offset={}", 
                paymentResponse.getPaymentId(), topic, partition, offset);

        try {
            // Forward the response to the final payment-response topic
            kafkaTemplate.send(paymentResponseTopic, paymentResponse.getPaymentId(), paymentResponse);
            log.info("Successfully forwarded payment response to final topic: paymentId={}", 
                    paymentResponse.getPaymentId());

            // Acknowledge the message after successful processing
            acknowledgment.acknowledge();

        } catch (Exception exception) {
            log.error("Failed to forward payment execution response: paymentId={}, error={}", 
                    paymentResponse.getPaymentId(), exception.getMessage(), exception);

            // In this case, we still acknowledge to prevent infinite reprocessing
            // The external system is assumed to be reliable, so this should be rare
            acknowledgment.acknowledge();
        }
    }
}