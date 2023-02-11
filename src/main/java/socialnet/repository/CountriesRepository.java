package socialnet.repository;

import socialnet.model.entities.Country;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CountriesRepository extends JpaRepository<Country, Long> {

    Optional<Country> findCountryByName(String country);
}
