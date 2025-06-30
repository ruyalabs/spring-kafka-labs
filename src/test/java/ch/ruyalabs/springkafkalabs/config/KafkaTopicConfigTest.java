package ch.ruyalabs.springkafkalabs.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "payment.kafka.topics.request=test-payment-requests",
    "payment.kafka.topics.response=test-payment-responses"
})
public class KafkaTopicConfigTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    public void testPaymentRequestTopicBeanExists() {
        // Verify that the paymentRequestTopic bean exists
        assertTrue(applicationContext.containsBean("paymentRequestTopic"));
        
        NewTopic paymentRequestTopic = applicationContext.getBean("paymentRequestTopic", NewTopic.class);
        assertNotNull(paymentRequestTopic);
        assertEquals("test-payment-requests", paymentRequestTopic.name());
        assertEquals(3, paymentRequestTopic.numPartitions());
        assertEquals(1, paymentRequestTopic.replicationFactor());
    }

    @Test
    public void testPaymentResponseTopicBeanExists() {
        // Verify that the paymentResponseTopic bean exists
        assertTrue(applicationContext.containsBean("paymentResponseTopic"));
        
        NewTopic paymentResponseTopic = applicationContext.getBean("paymentResponseTopic", NewTopic.class);
        assertNotNull(paymentResponseTopic);
        assertEquals("test-payment-responses", paymentResponseTopic.name());
        assertEquals(3, paymentResponseTopic.numPartitions());
        assertEquals(1, paymentResponseTopic.replicationFactor());
    }

    @Test
    public void testKafkaTopicConfigBeanExists() {
        // Verify that the KafkaTopicConfig bean exists
        assertTrue(applicationContext.containsBean("kafkaTopicConfig"));
        
        KafkaTopicConfig config = applicationContext.getBean(KafkaTopicConfig.class);
        assertNotNull(config);
    }
}