package ee.digit25.detector.domain.person;

import ee.digit25.detector.domain.person.external.PersonRequester;
import ee.digit25.detector.domain.person.external.api.Person;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PersonValidator {

    private final PersonRequester requester;

    public boolean isValid(String personCode) {
        Person person = requester.get(personCode);
        return !hasWarrantIssued(person) && hasContract(person) && !isBlacklisted(person);
    }

    private boolean hasWarrantIssued(Person person) {
        log.info("Checking if person ({}) has a warrant issued", person.getPersonCode());
        return person.getWarrantIssued();
    }

    private boolean hasContract(Person person) {
        log.info("Checking if person ({}) has a contract", person.getPersonCode());
        return person.getHasContract();
    }

    private boolean isBlacklisted(Person person) {
        log.info("Checking if person ({}) is blacklisted", person.getPersonCode());
        return person.getBlacklisted();
    }
}
