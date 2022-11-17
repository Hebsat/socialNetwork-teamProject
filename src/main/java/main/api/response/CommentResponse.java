package main.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import main.model.entities.Comment;
import main.service.CommentsService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class CommentResponse {

    private Long id;

    private LocalDateTime time;

    @JsonProperty("post_id")
    private Long postId;

    @JsonProperty("parent_id")
    private Long parentId;

    private PersonResponse author;

    @JsonProperty("comment_text")
    private String commentText;

    @JsonProperty("is_blocked")
    private Boolean isBlocked;

    @JsonProperty("is_deleted")
    private Boolean isDeleted;

    @JsonProperty("sub_comments")
    private List<CommentResponse> embeddedComments;

    private Integer likes;

    private Boolean myLike;

    public CommentResponse(Comment comment) {
        this.id = comment.getId();
        this.time = comment.getTime();
        this.postId = comment.getPost() == null ? null : comment.getPost().getId();
        this.parentId = comment.getParentComment() == null ? null : comment.getParentComment().getId();
        this.author = new PersonResponse(comment.getPerson());
        this.commentText = comment.getCommentText();
        this.isBlocked = comment.getIsBlocked();
        this.isDeleted = comment.getIsDeleted();
//        this.embeddedComments = new ArrayList<>(CommentsService.commentsToResponse(comment.getEmbeddedComments()));
        this.likes = 0;
        this.myLike = false;
    }
}
