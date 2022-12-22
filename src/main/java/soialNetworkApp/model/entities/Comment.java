package soialNetworkApp.model.entities;

import lombok.Data;
import soialNetworkApp.model.entities.interfaces.Liked;
import soialNetworkApp.model.entities.interfaces.Notificationed;
import soialNetworkApp.model.enums.LikeTypes;
import soialNetworkApp.model.enums.NotificationTypes;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Table(name = "post_comments")
public class Comment implements Liked, Notificationed {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime time;

    @ManyToOne
    @JoinColumn(name = "post_id")
    private Post post;

    @ManyToOne
    @JoinColumn(name = "parent_id")
    private Comment parentComment;

    @OneToMany(mappedBy = "parentComment",cascade = CascadeType.ALL)
    private List<Comment> embeddedComments = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "author_id", nullable = false)
    private Person author;

    @Column(name = "comment_text", nullable = false, columnDefinition = "TEXT")
    private String commentText;

    @Column(name = "is_blocked", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isBlocked;

    @Column(name = "is_deleted", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isDeleted;

    @Override
    public LikeTypes getType() {
        return LikeTypes.COMMENT;
    }

    @Override
    public NotificationTypes getNotificationType() {
        return post == null ? NotificationTypes.COMMENT_COMMENT : NotificationTypes.POST_COMMENT;
    }

    @Override
    public String toString() {
        return "Comment{" +
                "id=" + id +
                ", postId=" + (post == null ? "no post" : post.getId()) +
                ", parentCommentId=" + (parentComment == null ? "no parent comment" : parentComment.getId()) +
                ", embeddedCommentsCount=" + embeddedComments.size() +
                ", authorId=" + author.getId() +
                ", isBlocked=" + isBlocked +
                ", isDeleted=" + isDeleted +
                '}';
    }
}