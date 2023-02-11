package socialnet.service;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import socialnet.model.entities.Person;
import socialnet.repository.PersonsRepository;

@Service
@CacheConfig(cacheNames = "cache-person")
@RequiredArgsConstructor
public class PersonCacheService {
    private final PersonsRepository personsRepository;

    @CachePut(value="persons",key = "#person.email")
    public Person cachePerson(Person person){
        personsRepository.save(person);
        return person;
    }
    public Person getPersonById(long personId) {
        return getPersonByEmail(personsRepository.findPersonById(personId).get().getEmail());
    }

    @Cacheable(value="persons")
    public Person getPersonByEmail(String email) {
        return personsRepository.findPersonByEmail(email).orElse(null);
    }

    public Person getPersonByContext() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return getPersonByEmail(email);
    }
}

