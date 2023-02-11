package socialnet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import socialnet.model.entities.City;
import socialnet.model.entities.Country;

import java.util.List;
import java.util.Optional;

@Repository
public interface CitiesRepository extends JpaRepository<City, Long> {

    @Query("SELECT DISTINCT gismeteoId FROM City")
    List<Integer> findGismeteoIds();

    List<City> findCitiesByCountryAndNameStartsWith(Country country, String startsWith);

    Optional<City> findCityByNameAndDistrictAndSubDistrict(String name, String district, String subDistrict);
}
