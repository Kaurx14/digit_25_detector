package ee.digit25.detector.domain.transaction.external;

import ee.bitweb.core.retrofit.RetrofitRequestExecutor;
import ee.digit25.detector.domain.transaction.external.api.Transaction;
import ee.digit25.detector.domain.transaction.external.api.TransactionApiProperties;
import ee.digit25.detector.domain.transaction.external.api.TransactionsApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionRequester {

    private final TransactionsApi api;
    private final TransactionApiProperties properties;

    public List<Transaction> getUnverified(int amount) {
        log.info("Requesting a batch of unverified transactions of size {}", amount);
        return RetrofitRequestExecutor.executeRaw(api.getUnverified(properties.getToken(), amount));
    }

}
