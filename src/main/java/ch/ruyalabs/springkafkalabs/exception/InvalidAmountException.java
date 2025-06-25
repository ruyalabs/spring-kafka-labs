package ch.ruyalabs.springkafkalabs.exception;

/**
 * Exception thrown when a payment request contains an invalid amount (null or non-positive).
 */
public class InvalidAmountException extends PaymentValidationException {
    
    public InvalidAmountException(String message) {
        super(message);
    }
    
    public InvalidAmountException(String message, Throwable cause) {
        super(message, cause);
    }
}