package main.model.entities;
import lombok.Data;
import main.model.enums.LikeTypes;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.ManyToAny;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@Table(name = "post_comments")
public class Comment implements Liked {

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

    @OneToMany(mappedBy = "parentComment")
    private List<Comment> embeddedComments;

    @ManyToOne
    @JoinColumn(name = "author_id", nullable = false)
    private Person person;

    @Column(name = "comment_text", nullable = false, columnDefinition = "TEXT")
    private String commentText;

    @Column(name = "is_blocked", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isBlocked;

    @Column(name = "is_deleted", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isDeleted;

//    @ManyToAny(metaDef = "likesMetaDef", metaColumn = @Column(name = "meta_column"))
//    @Cascade(org.hibernate.annotations.CascadeType.ALL)
//    @JoinTable(name = "post_likes", joinColumns = @JoinColumn(name = "entity_id"), inverseJoinColumns = @JoinColumn(name = "ent_id"))
//    private List<Like> likes;


    @Override
    public Person getAuthor() {
        return person;
    }

    @Override
    public LikeTypes getType() {
        return LikeTypes.COMMENT;
    }
}
