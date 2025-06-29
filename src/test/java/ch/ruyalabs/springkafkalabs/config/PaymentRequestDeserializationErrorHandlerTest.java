package ch.ruyalabs.springkafkalabs.config;

import ch.ruyalabs.springkafkalabs.service.ReliableResponseProducer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PaymentRequestDeserializationErrorHandlerTest {

    @Mock
    private ReliableResponseProducer reliableResponseProducer;

    @Test
    void testErrorHandlerExists() {
        // Simple test to verify the error handler can be instantiated
        PaymentRequestDeserializationErrorHandler errorHandler = 
            new PaymentRequestDeserializationErrorHandler(reliableResponseProducer);
        assertNotNull(errorHandler);
    }
}