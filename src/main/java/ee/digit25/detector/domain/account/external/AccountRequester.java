package ee.digit25.detector.domain.account.external;

import ee.bitweb.core.retrofit.RetrofitRequestExecutor;
import ee.digit25.detector.domain.account.external.api.Account;
import ee.digit25.detector.domain.account.external.api.AccountApi;
import ee.digit25.detector.domain.account.external.api.AccountApiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountRequester {

    private final AccountApi api;
    private final AccountApiProperties properties;
    private final Map<String, CacheEntry<Account>> accountCache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = TimeUnit.MINUTES.toMillis(5);

    public Account get(String accountNumber) {
        Account cachedAccount = getCachedAccount(accountNumber);
        if (cachedAccount != null) {
            log.info("Returning cached account {}", accountNumber);
            return cachedAccount;
        }

        log.info("Requesting account {}", accountNumber);
        Account account = RetrofitRequestExecutor.executeRaw(api.get(properties.getToken(), accountNumber));
        cacheAccount(accountNumber, account);
        return account;
    }

    public List<Account> get(List<String> numbers) {
        log.info("Requesting accounts with numbers {}", numbers);
        List<Account> accounts = RetrofitRequestExecutor.executeRaw(api.get(properties.getToken(), numbers));
        
        // Cache all fetched accounts
        accounts.forEach(account -> cacheAccount(account.getNumber(), account));
        
        return accounts;
    }

    public List<Account> get(int pageNumber, int pageSize) {
        log.info("Requesting accounts page {} of size {}", pageNumber, pageSize);
        List<Account> accounts = RetrofitRequestExecutor.executeRaw(api.get(properties.getToken(), pageNumber, pageSize));
        
        // Cache all fetched accounts
        accounts.forEach(account -> cacheAccount(account.getNumber(), account));
        
        return accounts;
    }
    
    private Account getCachedAccount(String accountNumber) {
        CacheEntry<Account> entry = accountCache.get(accountNumber);
        if (entry != null && !entry.isExpired()) {
            return entry.getValue();
        }
        return null;
    }
    
    private void cacheAccount(String accountNumber, Account account) {
        accountCache.put(accountNumber, new CacheEntry<>(account, System.currentTimeMillis() + CACHE_TTL_MS));
    }
    
    private static class CacheEntry<T> {
        private final T value;
        private final long expiryTime;
        
        public CacheEntry(T value, long expiryTime) {
            this.value = value;
            this.expiryTime = expiryTime;
        }
        
        public T getValue() {
            return value;
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }
}
