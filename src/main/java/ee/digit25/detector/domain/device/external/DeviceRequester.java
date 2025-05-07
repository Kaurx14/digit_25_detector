package ee.digit25.detector.domain.device.external;

import ee.bitweb.core.retrofit.RetrofitRequestExecutor;
import ee.digit25.detector.domain.device.external.api.Device;
import ee.digit25.detector.domain.device.external.api.DeviceApi;
import ee.digit25.detector.domain.device.external.api.DeviceApiProperties;
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
public class DeviceRequester {

    private final DeviceApi api;
    private final DeviceApiProperties properties;
    private final Map<String, CacheEntry<Device>> deviceCache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = TimeUnit.MINUTES.toMillis(5);

    public Device get(String mac) {
        Device cachedDevice = getCachedDevice(mac);
        if (cachedDevice != null) {
            return cachedDevice;
        }

        Device device = RetrofitRequestExecutor.executeRaw(api.get(properties.getToken(), mac));
        cacheDevice(mac, device);
        return device;
    }

    public List<Device> get(List<String> macs) {
        List<Device> devices = RetrofitRequestExecutor.executeRaw(api.get(properties.getToken(), macs));
        
        devices.forEach(device -> cacheDevice(device.getMac(), device));
        
        return devices;
    }

    public List<Device> get(int pageNumber, int pageSize) {
        List<Device> devices = RetrofitRequestExecutor.executeRaw(api.get(properties.getToken(), pageNumber, pageSize));
        
        devices.forEach(device -> cacheDevice(device.getMac(), device));
        
        return devices;
    }
    
    private Device getCachedDevice(String mac) {
        CacheEntry<Device> entry = deviceCache.get(mac);
        if (entry != null && !entry.isExpired()) {
            return entry.getValue();
        }
        return null;
    }
    
    private void cacheDevice(String mac, Device device) {
        deviceCache.put(mac, new CacheEntry<>(device, System.currentTimeMillis() + CACHE_TTL_MS));
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
