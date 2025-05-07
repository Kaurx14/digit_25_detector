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
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class Processor {

    // Rate limiting constants
    private static final int MAX_CONCURRENT_REQUESTS = 50;
    private static final int MAX_PENDING_TRANSACTIONS = 10000;
    
    // Semaphore to control concurrent API calls
    private final Semaphore requestThrottle = new Semaphore(MAX_CONCURRENT_REQUESTS);
    
    // Counter to track pending transactions
    private final AtomicInteger pendingTransactions = new AtomicInteger(0);
    
    private final int TRANSACTION_BATCH_SIZE = 50; // Increased batch size - Lauri
    private final TransactionRequester requester;
    private final TransactionValidator validator;
    private final TransactionVerifier verifier;
    private final ExecutorService executorService = Executors.newFixedThreadPool(
            Math.max(Runtime.getRuntime().availableProcessors(), 10)); // Scale with available CPUs

    @Scheduled(fixedDelay = 10) // Run more frequently - Lauri
    public void process() {
        // Check if we're already at the pending transactions limit
        int currentPending = pendingTransactions.get();
        if (currentPending >= MAX_PENDING_TRANSACTIONS) {
            // We've reached the limit, skip this batch
            return;
        }
        
        // Calculate how many more we can process
        int availableCapacity = MAX_PENDING_TRANSACTIONS - currentPending;
        int batchSize = Math.min(TRANSACTION_BATCH_SIZE, availableCapacity);
        
        // Try to acquire permits for concurrent processing
        if (!requestThrottle.tryAcquire(1)) {
            // No permits available, we're at the concurrent limit
            return;
        }
        
        try {
            List<Transaction> transactions = requester.getUnverified(batchSize);
            if (transactions.isEmpty()) {
                return;
            }
            
            // Update pending count
            pendingTransactions.addAndGet(transactions.size());

            List<Transaction> legitimateTransactions = new ArrayList<>();
            List<Transaction> fraudulentTransactions = new ArrayList<>();

            // Process transactions in parallel
            List<CompletableFuture<Void>> futures = transactions.stream()
                    .map(transaction -> CompletableFuture.runAsync(() -> {
                        try {
                            if (validator.isLegitimate(transaction)) {
                                synchronized (legitimateTransactions) {
                                    legitimateTransactions.add(transaction);
                                }
                            } else {
                                synchronized (fraudulentTransactions) {
                                    fraudulentTransactions.add(transaction);
                                }
                            }
                        } catch (Exception e) {
                            // In case of exception, add to fraudulent
                            synchronized (fraudulentTransactions) {
                                fraudulentTransactions.add(transaction);
                            }
                        }
                    }, executorService))
                    .collect(Collectors.toList());

            // Wait for all validations to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            // Process legitimate transactions
            if (!legitimateTransactions.isEmpty()) {
                try {
                    requestThrottle.acquire(1); // Acquire a permit for the API call
                    try {
                        verifier.verify(legitimateTransactions);
                    } finally {
                        requestThrottle.release(1); // Always release the permit
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            
            // Process fraudulent transactions
            if (!fraudulentTransactions.isEmpty()) {
                try {
                    requestThrottle.acquire(1); // Acquire a permit for the API call
                    try {
                        verifier.reject(fraudulentTransactions);
                    } finally {
                        requestThrottle.release(1); // Always release the permit
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            
            // Update pending count after processing
            pendingTransactions.addAndGet(-(legitimateTransactions.size() + fraudulentTransactions.size()));
            
        } catch (Exception e) {
            // Handle any unexpected errors
        } finally {
            // Always release the main permit
            requestThrottle.release(1);
        }
    }
}
