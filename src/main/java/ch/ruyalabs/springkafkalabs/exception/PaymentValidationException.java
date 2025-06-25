package ch.ruyalabs.springkafkalabs.exception;

/**
 * Base exception class for payment validation errors.
 * All specific payment validation exceptions should extend this class.
 */
public class PaymentValidationException extends Exception {
    
    public PaymentValidationException(String message) {
        super(message);
    }
    
    public PaymentValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}