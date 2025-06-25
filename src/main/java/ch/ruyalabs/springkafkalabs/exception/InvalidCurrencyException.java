package ch.ruyalabs.springkafkalabs.exception;

/**
 * Exception thrown when a payment request contains an unsupported currency.
 */
public class InvalidCurrencyException extends PaymentValidationException {
    
    public InvalidCurrencyException(String message) {
        super(message);
    }
    
    public InvalidCurrencyException(String message, Throwable cause) {
        super(message, cause);
    }
}