package ee.digit25.detector.process;

import ee.digit25.detector.domain.transaction.TransactionValidator;
import ee.digit25.detector.domain.transaction.external.TransactionRequester;
import ee.digit25.detector.domain.transaction.external.TransactionVerifier;
import ee.digit25.detector.domain.transaction.external.api.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class Processor {

    private final int TRANSACTION_BATCH_SIZE = 20; // Increased batch size - Lauri
    private final TransactionRequester requester;
    private final TransactionValidator validator;
    private final TransactionVerifier verifier;
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    @Scheduled(fixedDelay = 500) // Run more frequently - Lauri
    public void process() {
        log.info("Starting to process a batch of transactions of size {}", TRANSACTION_BATCH_SIZE);

        List<Transaction> transactions = requester.getUnverified(TRANSACTION_BATCH_SIZE);
        if (transactions.isEmpty()) {
            return;
        }

        List<Transaction> legitimateTransactions = new ArrayList<>();
        List<Transaction> fraudulentTransactions = new ArrayList<>();

        // Process transactions in parallel
        List<CompletableFuture<Void>> futures = transactions.stream()
                .map(transaction -> CompletableFuture.runAsync(() -> {
                    if (validator.isLegitimate(transaction)) {
                        log.info("Legitimate transaction {}", transaction.getId());
                        synchronized (legitimateTransactions) {
                            legitimateTransactions.add(transaction);
                        }
                    } else {
                        log.info("Not legitimate transaction {}", transaction.getId());
                        synchronized (fraudulentTransactions) {
                            fraudulentTransactions.add(transaction);
                        }
                    }
                }, executorService))
                .collect(Collectors.toList());

        // Wait for all validations to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // Batch verify/reject
        if (!legitimateTransactions.isEmpty()) {
            verifier.verify(legitimateTransactions);
        }
        
        if (!fraudulentTransactions.isEmpty()) {
            verifier.reject(fraudulentTransactions);
        }
    }
}
