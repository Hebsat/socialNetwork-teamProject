package socialnet.repository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import socialnet.model.entities.Person;
import socialnet.model.entities.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PostsRepository extends JpaRepository<Post, Long>, JpaSpecificationExecutor<Post> {

    Page<Post> findPostsByAuthorAndIsBlockedFalseOrderByTimeDesc(Pageable pageable, Person author);

    Page<Post> findPostsByTimeBeforeAndIsDeletedFalseAndIsBlockedFalseOrderByTimeDesc(Pageable pageable, LocalDateTime time);

    @Query(value = "SELECT * FROM posts WHERE author_id = :id", nativeQuery = true)
    List<Post> findPostsToDelete(@Param("id") long id);

    Long countAllByAuthorId(long authorId);

    Page<Post> findPostsByAuthorIsNotInAndIsDeletedFalseOrderByTimeDesc(Pageable pageable, List<Person> blockedPersons);
}
