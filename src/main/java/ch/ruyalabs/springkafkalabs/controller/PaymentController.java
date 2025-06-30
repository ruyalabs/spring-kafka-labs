package ch.ruyalabs.springkafkalabs.controller;

import ch.ruyalabs.springkafkalabs.kafka.producer.PaymentRequestProducer;
import ch.ruyalabs.types.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Date;
import java.util.UUID;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentRequestProducer paymentRequestProducer;

    @Autowired
    public PaymentController(PaymentRequestProducer paymentRequestProducer) {
        this.paymentRequestProducer = paymentRequestProducer;
    }

    @PostMapping("/trigger")
    public ResponseEntity<String> triggerPaymentRequest(HttpServletRequest httpRequest) {
        try {
            logAllHttpHeaders(httpRequest);

            PaymentDisbursementRequest request = createDummyPaymentRequest();
            paymentRequestProducer.sendPaymentRequest(request);
            return ResponseEntity.ok("Payment request sent successfully with disbursementId: " + request.getDisbursementId());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to send payment request: " + e.getMessage());
        }
    }

    private PaymentDisbursementRequest createDummyPaymentRequest() {
        PaymentDisbursementRequest request = new PaymentDisbursementRequest();

        request.setDisbursementId(UUID.randomUUID());

        PaymentRecipient recipient = new PaymentRecipient();
        recipient.setName("John Doe");
        recipient.setEmail("john.doe@example.com");

        BankAccountDetails bankDetails = new BankAccountDetails();
        bankDetails.setAccountNumber("9876543210");
        bankDetails.setSortCode("44-55-66");
        bankDetails.setIban("GB29NWBK60161331926819");
        recipient.setBankDetails(bankDetails);

        request.setRecipient(recipient);

        PaymentAmount amount = new PaymentAmount();
        amount.setValue(2500.50);
        amount.setCurrency("USD");
        request.setAmount(amount);

        request.setPaymentMethod(PaymentDisbursementRequest.PaymentMethod.BANK_TRANSFER);
        request.setRequestedAt(new Date());

        PaymentMetadata metadata = new PaymentMetadata();
        metadata.setAdditionalProperty("internalReference", "TXN-" + UUID.randomUUID().toString().substring(0, 8));
        metadata.setAdditionalProperty("notes", "Dummy payment request triggered via REST API");
        request.setMetadata(metadata);

        return request;
    }

    private void logAllHttpHeaders(HttpServletRequest httpRequest) {
        logger.info("HTTP request headers for endpoint: {} {}", httpRequest.getMethod(), httpRequest.getRequestURI());

        Collections.list(httpRequest.getHeaderNames()).forEach(headerName -> {
            Collections.list(httpRequest.getHeaders(headerName)).forEach(headerValue -> {
                logger.info("HTTP Header - Key: {}, Value: {}", headerName, headerValue);
            });
        });

        logger.info("Request Info - Remote Address: {}, User Agent: {}, Content Type: {}",
                   httpRequest.getRemoteAddr(), 
                   httpRequest.getHeader("User-Agent"), 
                   httpRequest.getContentType());
    }
}
