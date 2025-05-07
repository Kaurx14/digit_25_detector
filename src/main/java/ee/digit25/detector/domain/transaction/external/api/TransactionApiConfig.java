package ee.digit25.detector.domain.transaction.external.api;

import ee.bitweb.core.retrofit.builder.SpringAwareRetrofitBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class TransactionApiConfig {

    private final SpringAwareRetrofitBuilder builder;
    private final TransactionApiProperties properties;

    @Bean
    public TransactionsApi transctionsApi() {

        return builder.create(properties.getBaseUrl(), TransactionsApi.class).build();
    }
}
