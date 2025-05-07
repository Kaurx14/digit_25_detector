package ee.digit25.detector.domain.account;

import ee.digit25.detector.domain.account.external.AccountRequester;
import ee.digit25.detector.domain.account.external.api.Account;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountValidator {

    private final AccountRequester requester;

    public boolean isValidSenderAccount(String accountNumber, BigDecimal amount, String senderPersonCode) {
        log.info("Checking if account {} is valid sender account", accountNumber);
        Account account = requester.get(accountNumber);
        return !isClosed(account) && isOwner(account, senderPersonCode) && hasBalance(account, amount);
    }

    public boolean isValidRecipientAccount(String accountNumber, String recipientPersonCode) {
        log.info("Checking if account {} is valid recipient account", accountNumber);
        Account account = requester.get(accountNumber);
        return !isClosed(account) && isOwner(account, recipientPersonCode);
    }

    private boolean isOwner(Account account, String personCode) {
        log.info("Checking if {} is owner of account {}", personCode, account.getNumber());
        return personCode.equals(account.getOwner());
    }

    private boolean hasBalance(Account account, BigDecimal amount) {
        log.info("Checking if account {} has balance for amount {}", account.getNumber(), amount);
        return account.getBalance().compareTo(amount) >= 0;
    }

    private boolean isClosed(Account account) {
        log.info("Checking if account {} is closed", account.getNumber());
        return account.getClosed();
    }
}
