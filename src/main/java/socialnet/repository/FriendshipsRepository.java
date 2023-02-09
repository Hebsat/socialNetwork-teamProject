package socialnet.repository;

import socialnet.model.entities.Friendship;
import socialnet.model.entities.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import socialnet.model.enums.FriendshipStatusTypes;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendshipsRepository extends JpaRepository<Friendship, Long> {

    List<Friendship> findFriendshipByFriendshipStatus(FriendshipStatusTypes friendshipStatusTypes);

    List<Friendship> findFriendshipBySrcPerson(Person srcPerson);

    List<Friendship> findFriendshipById(Long id);

    @Query(value = "SELECT * FROM friendships WHERE (dst_person_id = :id OR src_person_id = :id)",
            nativeQuery = true)
    List<Friendship> findFriendsToDelete(@Param("id") long id);

    List<Friendship> findFriendshipsByDstPerson(Person dstPerson);

    Optional <Friendship> findFriendshipBySrcPersonIdAndDstPersonId(Long srcPersonId, Long dstPersonId);

    Optional<Friendship>findFriendshipBySrcPersonAndDstPerson(Person srcPerson, Person dstPerson);

    List<Friendship> findFriendshipsByDstPersonIdAndFriendshipStatus(Long id, FriendshipStatusTypes status);

    @Query("FROM Friendship AS f " +
            "WHERE f.friendshipStatus = :status " +
            "AND (f.srcPerson.id = :srcId OR f.dstPerson.id = :dstId)")
    List<Friendship> findFriendshipsByFriendshipStatusAndSrcPersonIdOrDstPersonId(@Param(value = "status") FriendshipStatusTypes status,
                                                                                  @Param(value = "srcId") Long srcId,
                                                                                  @Param(value = "dstId") Long dstId);
    Optional<Friendship> findFriendshipByFriendshipStatusAndSrcPersonIdAndDstPersonId(FriendshipStatusTypes status, Long srcId, Long dstId);
}
