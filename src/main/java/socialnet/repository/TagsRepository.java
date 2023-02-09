package socialnet.repository;

import socialnet.model.entities.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TagsRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findByTagName(String tagName);
}

