package ee.digit25.detector.domain.transaction;

import ee.digit25.detector.domain.account.AccountValidator;
import ee.digit25.detector.domain.device.DeviceValidator;
import ee.digit25.detector.domain.person.PersonValidator;
import ee.digit25.detector.domain.transaction.external.api.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionValidator {

    private final PersonValidator personValidator;
    private final DeviceValidator deviceValidator;
    private final AccountValidator accountValidator;
    private final ConcurrentHashMap<String, Boolean> validationCache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL = 300_000; // 5 minutes in ms

    private class ValidationCacheKey {
        private final String transactionId;
        private final long timestamp;

        public ValidationCacheKey(String transactionId) {
            this.transactionId = transactionId;
            this.timestamp = System.currentTimeMillis();
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL;
        }
    }

    public boolean isLegitimate(Transaction transaction) {
        String transactionId = transaction.getId();
        
        // Check if we've already validated this transaction
        Boolean cachedResult = validationCache.get(transactionId);
        if (cachedResult != null) {
            return cachedResult;
        }

        // Perform parallel validations for better performance
        CompletableFuture<Boolean> deviceValidation = CompletableFuture.supplyAsync(
                () -> deviceValidator.isValid(transaction.getDeviceMac()));
        
        CompletableFuture<Boolean> senderValidation = CompletableFuture.supplyAsync(
                () -> personValidator.isValid(transaction.getSender()));
        
        CompletableFuture<Boolean> recipientValidation = CompletableFuture.supplyAsync(
                () -> personValidator.isValid(transaction.getRecipient()));
        
        try {
            // If device, sender or recipient validation fails, we can return early
            if (!deviceValidation.get() || !senderValidation.get() || !recipientValidation.get()) {
                validationCache.put(transactionId, false);
                return false;
            }
            
            // Perform account validations, which depend on personal data
            boolean senderAccountValid = accountValidator.isValidSenderAccount(
                    transaction.getSenderAccount(), 
                    transaction.getAmount(), 
                    transaction.getSender());
            
            if (!senderAccountValid) {
                validationCache.put(transactionId, false);
                return false;
            }
            
            boolean recipientAccountValid = accountValidator.isValidRecipientAccount(
                    transaction.getRecipientAccount(), 
                    transaction.getRecipient());
            
            boolean result = recipientAccountValid;
            validationCache.put(transactionId, result);
            return result;
            
        } catch (InterruptedException | ExecutionException e) {
            // If there's an exception, we assume the transaction is not legitimate
            validationCache.put(transactionId, false);
            return false;
        }
    }
}
