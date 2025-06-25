package ch.ruyalabs.springkafkalabs.consumer;

import ch.ruyalabs.springkafkalabs.dto.PaymentResponseDto;
import ch.ruyalabs.springkafkalabs.service.ReliableResponseProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentExecutionResponseConsumer {

    private final ReliableResponseProducer reliableResponseProducer;

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

        reliableResponseProducer.sendResponse(paymentResponseTopic, paymentResponse.getPaymentId(), paymentResponse);

        acknowledgment.acknowledge();
    }
}
