package ch.ruyalabs.springkafkalabs.controller;

import ch.ruyalabs.springkafkalabs.kafka.producer.PaymentRequestProducer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
public class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentRequestProducer paymentRequestProducer;

    @Test
    public void testTriggerPaymentRequest() throws Exception {
        // Given
        doNothing().when(paymentRequestProducer).sendPaymentRequest(any());

        // When & Then
        mockMvc.perform(post("/api/payment/trigger"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Payment request sent successfully with disbursementId:")));
    }
}
