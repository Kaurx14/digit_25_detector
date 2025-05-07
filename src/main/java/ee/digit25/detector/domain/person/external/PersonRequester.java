package ee.digit25.detector.domain.person.external;

import ee.bitweb.core.retrofit.RetrofitRequestExecutor;
import ee.digit25.detector.domain.person.external.api.Person;
import ee.digit25.detector.domain.person.external.api.PersonApi;
import ee.digit25.detector.domain.person.external.api.PersonApiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class PersonRequester {

    private final PersonApi api;
    private final PersonApiProperties properties;
    private final Map<String, CacheEntry<Person>> personCache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = TimeUnit.MINUTES.toMillis(5);

    public Person get(String personCode) {
        Person cachedPerson = getCachedPerson(personCode);
        if (cachedPerson != null) {
            return cachedPerson;
        }

        Person person = RetrofitRequestExecutor.executeRaw(api.get(properties.getToken(), personCode));
        cachePerson(personCode, person);
        return person;
    }

    public List<Person> get(List<String> personCodes) {
        List<Person> persons = RetrofitRequestExecutor.executeRaw(api.get(properties.getToken(), personCodes));
        
        // Cache all fetched persons
        persons.forEach(person -> cachePerson(person.getPersonCode(), person));
        
        return persons;
    }

    public List<Person> get(int pageNumber, int pageSize) {
        List<Person> persons = RetrofitRequestExecutor.executeRaw(api.get(properties.getToken(), pageNumber, pageSize));
        
        // Cache all fetched persons
        persons.forEach(person -> cachePerson(person.getPersonCode(), person));
        
        return persons;
    }
    
    private Person getCachedPerson(String personCode) {
        CacheEntry<Person> entry = personCache.get(personCode);
        if (entry != null && !entry.isExpired()) {
            return entry.getValue();
        }
        return null;
    }
    
    private void cachePerson(String personCode, Person person) {
        personCache.put(personCode, new CacheEntry<>(person, System.currentTimeMillis() + CACHE_TTL_MS));
    }
    
    private static class CacheEntry<T> {
        private final T value;
        private final long expiryTime;
        
        public CacheEntry(T value, long expiryTime) {
            this.value = value;
            this.expiryTime = expiryTime;
        }
        
        public T getValue() {
            return value;
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }
}
