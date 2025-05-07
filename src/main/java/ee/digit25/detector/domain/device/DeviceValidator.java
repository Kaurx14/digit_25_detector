package ee.digit25.detector.domain.device;

import ee.digit25.detector.domain.device.external.DeviceRequester;
import ee.digit25.detector.domain.device.external.api.Device;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceValidator {

    private final DeviceRequester requester;

    public boolean isValid(String mac) {
        log.info("Validating device {}", mac);
        Device device = requester.get(mac);
        return !isBlacklisted(device);
    }

    private boolean isBlacklisted(Device device) {
        log.info("Checking if device is blacklisted");
        return device.getIsBlacklisted();
    }
}
