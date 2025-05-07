package ee.digit25.detector.domain.device;

import ee.digit25.detector.domain.device.external.DeviceRequester;
import ee.digit25.detector.domain.device.external.api.Device;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceValidator {

    private final DeviceRequester requester;
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

    public boolean isValid(String mac) {
        ValidationResult cachedResult = validationCache.get(mac);
        if (cachedResult != null && !cachedResult.isExpired()) {
            return cachedResult.isValid();
        }
        
        Device device = requester.get(mac);
        boolean result = !isBlacklisted(device);
        
        validationCache.put(mac, new ValidationResult(result));
        return result;
    }

    private boolean isBlacklisted(Device device) {
        return device.getIsBlacklisted();
    }
}
