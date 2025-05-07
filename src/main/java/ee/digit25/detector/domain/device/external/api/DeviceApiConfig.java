package ee.digit25.detector.domain.device.external.api;

import ee.bitweb.core.retrofit.builder.SpringAwareRetrofitBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DeviceApiConfig {

    private final SpringAwareRetrofitBuilder builder;
    private final DeviceApiProperties properties;

    @Bean
    public DeviceApi deviceApi() {
        log.info("Creating Device API");

        return builder.create(properties.getBaseUrl(), DeviceApi.class).build();
    }
}
