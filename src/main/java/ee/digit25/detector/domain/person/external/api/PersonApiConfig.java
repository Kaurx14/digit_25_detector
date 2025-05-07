package ee.digit25.detector.domain.person.external.api;

import ee.bitweb.core.retrofit.builder.SpringAwareRetrofitBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class PersonApiConfig {

    private final SpringAwareRetrofitBuilder builder;
    private final PersonApiProperties properties;

    @Bean
    public PersonApi personApi() {
        log.info("Creating Persons API");

        return builder.create(properties.getBaseUrl(), PersonApi.class).build();
    }
}
