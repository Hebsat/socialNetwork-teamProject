package socialnet.repository;
import socialnet.model.entities.Message;
import socialnet.model.entities.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import socialnet.model.enums.ReadStatusTypes;

import java.time.ZonedDateTime;
import java.util.List;

@Repository
public interface MessagesRepository extends JpaRepository<Message, Long> {
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM messages WHERE (author_id = :id OR recipient_id = :id)",
            nativeQuery = true)
    void messagesDelete(@Param("id") long id);

    List<Message> findAllByRecipientAndIsDeletedFalse(Person recipient);
    List<Message> findAllByRecipientAndReadStatusAndIsDeletedFalse(Person person, ReadStatusTypes status);
    List<Message> findAllByDialogIdAndRecipientAndReadStatusAndIsDeletedFalse(Long dialogId, Person person, ReadStatusTypes status);

    List<Message> findAllByDialogIdAndIsDeletedFalseOrderByTimeAsc(Long dialogId);
    List<Message> findAllByDialogIdAndIsDeletedFalse(Long dialogId);
    Long countAllByDialogId(long id);
    Long countAllByAuthorIdAndRecipientId(long authorId, long recipientId);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO messages (time, message_text, read_status, is_deleted, author_id, recipient_id, dialog_id)" +
            " VALUES (:time, :messageText, :readStatus, :isDeleted, :authorId, :recipientId, :dialogId)", nativeQuery = true)
    void save(@Param("time") ZonedDateTime time, @Param("messageText") String messageText,
              @Param("readStatus") String readStatus, @Param("isDeleted") Boolean isDeleted,
              @Param("authorId") Long authorId, @Param("recipientId") Long recipientId, @Param("dialogId") Long dialogId);
}
