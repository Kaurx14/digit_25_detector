package ee.digit25.detector.domain.person;

import ee.digit25.detector.domain.person.external.PersonRequester;
import ee.digit25.detector.domain.person.external.api.Person;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class PersonValidator {

    private final PersonRequester requester;
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

    public boolean isValid(String personCode) {
        ValidationResult cachedResult = validationCache.get(personCode);
        if (cachedResult != null && !cachedResult.isExpired()) {
            return cachedResult.isValid();
        }
        
        Person person = requester.get(personCode);
        boolean result = !hasWarrantIssued(person) && hasContract(person) && !isBlacklisted(person);
        
        validationCache.put(personCode, new ValidationResult(result));
        return result;
    }

    private boolean hasWarrantIssued(Person person) {
        return person.getWarrantIssued();
    }

    private boolean hasContract(Person person) {
        return person.getHasContract();
    }

    private boolean isBlacklisted(Person person) {
        return person.getBlacklisted();
    }
}
