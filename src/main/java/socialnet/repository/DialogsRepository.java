package socialnet.repository;
import socialnet.model.entities.Dialog;
import socialnet.model.entities.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface DialogsRepository extends JpaRepository<Dialog, Long> {
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM dialogs WHERE (first_person_id = :id OR second_person_id = :id)",
            nativeQuery = true)
    void dialogsDelete(@Param("id") long id);
    Optional<Dialog> findDialogByFirstPersonAndSecondPerson(Person first, Person second);
    List<Dialog> findAllByFirstPersonAndIsDeletedFalseOrSecondPersonAndIsDeletedFalse(Person firstPerson, Person secondPerson);
    Long countAllByFirstPersonAndIsDeletedFalseOrSecondPersonAndIsDeletedFalse(Person first, Person second);
    Long countAllByFirstPersonIdAndIsDeletedFalseOrSecondPersonIdAndIsDeletedFalse(long firstPerson, long secondPerson);
}
