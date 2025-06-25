package ch.ruyalabs.springkafkalabs.exception;

/**
 * Exception thrown when a payment request has a missing or empty payment ID.
 */
public class MissingPaymentIdException extends PaymentValidationException {
    
    public MissingPaymentIdException(String message) {
        super(message);
    }
    
    public MissingPaymentIdException(String message, Throwable cause) {
        super(message, cause);
    }
}