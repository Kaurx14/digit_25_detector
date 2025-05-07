package ee.digit25.detector.domain.account.external.api;

import ee.bitweb.core.retrofit.builder.SpringAwareRetrofitBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class AccountApiConfig {

    private final SpringAwareRetrofitBuilder builder;
    private final AccountApiProperties properties;

    @Bean
    public AccountApi accountApi() {
        log.info("Creating Accounts API");

        return builder.create(properties.getBaseUrl(), AccountApi.class).build();
    }
}
