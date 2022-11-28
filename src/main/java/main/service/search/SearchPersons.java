package main.service.search;

import lombok.RequiredArgsConstructor;
import main.api.request.FindPersonRq;
import main.model.entities.Person;
import main.repository.PersonsRepository;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SearchPersons {

    private final CommonSearchMethods commonSearchMethods;
    private final PersonsRepository personsRepository;
    private Long total;

    public List<Person> findPersons(FindPersonRq personRq, int offset, int perPage) throws SQLException {
        if (personRq.getFirst_name() != null && personRq.getLast_name() == null && personRq.getCity() == null &&
                personRq.getCountry() == null && personRq.getAge_to() == null && personRq.getAge_from() == null) {
            List<Person> persons = new ArrayList<>();
            persons.add(commonSearchMethods.findPersonByNameOrLastNameContains(personRq.getFirst_name()));
            return persons;
        }
        String ageQuery = getQueryForAge(personRq);
        ResultSet persons = commonSearchMethods.getStatement().executeQuery("SELECT p.id FROM persons AS p" +
                " JOIN cities c ON c.id = p.city_id" +
                " JOIN countries c2 on c2.id = c.country_id" +
                " WHERE" +
                (personRq.getFirst_name() != null ? " p.first_name ~* '" + personRq.getFirst_name() + "'" : "") +
                (personRq.getFirst_name() != null && personRq.getLast_name() != null ? " AND" : "") +
                (personRq.getLast_name() != null ? " p.last_name ~* '" + personRq.getLast_name() + "'" : "") +
                ((personRq.getFirst_name() != null || personRq.getLast_name() != null) && ageQuery != null ? " AND" : "") +
                (ageQuery != null ? ageQuery : "") +
                ((personRq.getLast_name() != null || personRq.getFirst_name() != null || ageQuery != null) && personRq.getCity() != null ? " AND" : "") +
                (personRq.getCity() != null ? " c.title ~* '" + personRq.getCity() + "'" : "") +
                ((personRq.getFirst_name() != null || personRq.getLast_name() != null || ageQuery != null || personRq.getCity() != null)
                        && personRq.getCountry() != null ? " AND" : "") +
                (personRq.getCountry() != null ? " c2.title ~* '" + personRq.getCountry() + "'" : ""));
        List<Long> personsId = commonSearchMethods.getIdsFromResultSet(persons);
        total = (long) personsId.size();
        commonSearchMethods.closeStatementAndConnection();
        persons.close();
        return commonSearchMethods.getPageFromList(personsRepository.findAllById(personsId), offset, perPage);
    }

    public Long getTotal() {
        return total;
    }

    private String getQueryForAge(FindPersonRq personRq) {
        LocalDateTime dateTimeFrom = LocalDateTime.now();
        LocalDateTime dateTimeTo = LocalDateTime.now();
        if (personRq.getAge_from() != null && personRq.getAge_to() == null) {
            dateTimeFrom = dateTimeFrom.minusYears(personRq.getAge_from());
            return " p.birth_date < '" + dateTimeFrom + "'";
        } else if (personRq.getAge_to() != null && personRq.getAge_from() == null) {
            dateTimeTo = dateTimeTo.minusYears(personRq.getAge_to());
            return " p.birth_date > '" + dateTimeTo + "'";
        } else if (personRq.getAge_from() != null && personRq.getAge_to() != null) {
            dateTimeTo = dateTimeTo.minusYears(personRq.getAge_to());
            dateTimeFrom = dateTimeFrom.minusYears(personRq.getAge_from());
            return " p.birth_date BETWEEN '" + dateTimeTo + "' AND '" + dateTimeFrom + "'";
        }
        return null;
    }
}