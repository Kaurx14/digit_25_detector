package ee.digit25.detector.domain.account;

import ee.digit25.detector.domain.account.external.AccountRequester;
import ee.digit25.detector.domain.account.external.api.Account;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountValidator {

    private final AccountRequester requester;
    private final Map<String, ValidationResult> validationCache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL = 300_000; // 5 minutes

    private static class ValidationResult {
        private final boolean isValid;
        private final long timestamp;

        public ValidationResult(boolean isValid) {
            this.isValid = isValid;
            this.timestamp = System.currentTimeMillis();
        }

        public boolean isValid() {
            return isValid;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL;
        }
    }

    public boolean isValidSenderAccount(String accountNumber, BigDecimal amount, String senderPersonCode) {
        String cacheKey = "sender:" + accountNumber + ":" + senderPersonCode + ":" + amount;
        ValidationResult cachedResult = validationCache.get(cacheKey);
        if (cachedResult != null && !cachedResult.isExpired()) {
            return cachedResult.isValid();
        }
        
        Account account = requester.get(accountNumber);
        boolean result = !isClosed(account) && isOwner(account, senderPersonCode) && hasBalance(account, amount);
        
        validationCache.put(cacheKey, new ValidationResult(result));
        return result;
    }

    public boolean isValidRecipientAccount(String accountNumber, String recipientPersonCode) {
        String cacheKey = "recipient:" + accountNumber + ":" + recipientPersonCode;
        ValidationResult cachedResult = validationCache.get(cacheKey);
        if (cachedResult != null && !cachedResult.isExpired()) {
            return cachedResult.isValid();
        }
        
        Account account = requester.get(accountNumber);
        boolean result = !isClosed(account) && isOwner(account, recipientPersonCode);
        
        validationCache.put(cacheKey, new ValidationResult(result));
        return result;
    }

    private boolean isOwner(Account account, String personCode) {
        return personCode.equals(account.getOwner());
    }

    private boolean hasBalance(Account account, BigDecimal amount) {
        return account.getBalance().compareTo(amount) >= 0;
    }

    private boolean isClosed(Account account) {
        return account.getClosed();
    }
}
