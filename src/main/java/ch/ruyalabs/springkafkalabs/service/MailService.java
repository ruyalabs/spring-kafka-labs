package ch.ruyalabs.springkafkalabs.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailService {

    /**
     * Simulates sending a notification email to the operations team.
     * This is used as the ultimate fallback mechanism when all other error handling fails.
     * 
     * @param paymentId The payment ID that failed to be processed
     * @param errorMessage The error message describing what went wrong
     * @param exception The exception that caused the failure (optional)
     */
    public void sendOperationsNotification(String paymentId, String errorMessage, Exception exception) {
        String logMessage = String.format(
            "[CRITICAL_PAYMENT_ERROR] OPERATIONS TEAM NOTIFICATION - " +
            "Payment ID: %s, Error: %s, Exception: %s, " +
            "Action Required: Manual investigation needed for payment processing failure",
            paymentId,
            errorMessage,
            exception != null ? exception.getMessage() : "No exception details"
        );
        
        log.error(logMessage, exception);
        
        // In a real implementation, this would send an actual email
        // For this simulation, we're just logging a clearly identifiable error message
        log.error("[SIMULATED_EMAIL_SENT] Operations team has been notified about payment ID: {}", paymentId);
    }
}