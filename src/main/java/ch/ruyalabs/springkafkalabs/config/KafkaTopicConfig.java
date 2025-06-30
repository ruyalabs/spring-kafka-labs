package ch.ruyalabs.springkafkalabs.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Value("${payment.kafka.topics.request}")
    private String paymentRequestTopic;

    @Value("${payment.kafka.topics.response}")
    private String paymentResponseTopic;

    @Bean
    public NewTopic paymentRequestTopic() {
        return TopicBuilder.name(paymentRequestTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic paymentResponseTopic() {
        return TopicBuilder.name(paymentResponseTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}