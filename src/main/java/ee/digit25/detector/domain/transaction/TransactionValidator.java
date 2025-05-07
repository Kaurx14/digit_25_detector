package ee.digit25.detector.domain.transaction;

import ee.digit25.detector.domain.account.AccountValidator;
import ee.digit25.detector.domain.device.DeviceValidator;
import ee.digit25.detector.domain.person.PersonValidator;
import ee.digit25.detector.domain.transaction.external.api.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionValidator {

    private final PersonValidator personValidator;
    private final DeviceValidator deviceValidator;
    private final AccountValidator accountValidator;

    public boolean isLegitimate(Transaction transaction) {
        // First validate the device since it's a quick check
        if (!deviceValidator.isValid(transaction.getDeviceMac())) {
            return false;
        }
        
        // Check sender person
        if (!personValidator.isValid(transaction.getSender())) {
            return false;
        }
        
        // Check recipient person
        if (!personValidator.isValid(transaction.getRecipient())) {
            return false;
        }
        
        // Check sender account
        if (!accountValidator.isValidSenderAccount(
                transaction.getSenderAccount(), 
                transaction.getAmount(), 
                transaction.getSender())) {
            return false;
        }
        
        // Check recipient account
        if (!accountValidator.isValidRecipientAccount(
                transaction.getRecipientAccount(), 
                transaction.getRecipient())) {
            return false;
        }
        
        return true;
    }
}
