package ch.ruyalabs.springkafkalabs.exception;

/**
 * Exception thrown when a payment request is marked as faulty for testing purposes.
 */
public class FaultyRequestException extends PaymentValidationException {
    
    public FaultyRequestException(String message) {
        super(message);
    }
    
    public FaultyRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}