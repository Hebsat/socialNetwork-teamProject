package main.service;

import lombok.RequiredArgsConstructor;
import main.api.request.CommentRequest;
import main.api.response.CommentResponse;
import main.api.response.CommonResponse;
import main.mappers.CommentMapper;
import main.model.entities.Comment;
import main.model.entities.Person;
import main.model.entities.Post;
import main.repository.CommentsRepository;
import main.service.util.NetworkPageRequest;
import org.mapstruct.Named;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentsService {

    private final CommentsRepository commentRepository;
    private final PersonsService personsService;
    private final CommentMapper commentMapper;

    public CommonResponse<CommentResponse> createComment(Post post, CommentRequest commentRequest) {
        Person person = personsService.getPersonByEmail((SecurityContextHolder.getContext().getAuthentication().getName()));
        Comment parentComment = getCommentById(commentRequest.getParentId());
        post = parentComment == null ? post : null;
        Comment comment = commentMapper.commentRequestToNewComment(commentRequest, post, person, parentComment, LocalDateTime.now());
        return CommonResponse.<CommentResponse>builder()
                .error("success")
                .timestamp(System.currentTimeMillis())
                .data(commentMapper.commentToResponse(commentRepository.save(comment)))
                .build();
    }

    public CommonResponse<List<CommentResponse>> getPostComments(Post post, int offset, int size) {
        Pageable pageable = NetworkPageRequest.of(offset, size);
        Page<Comment> commentPage = commentRepository.findCommentsByPostOrderByTimeAsc(pageable, post);
        return CommonResponse.<List<CommentResponse>>builder()
                .error("success")
                .timestamp(System.currentTimeMillis())
                .offset(offset)
                .itemPerPage(commentPage.getSize())
                .total(commentPage.getTotalElements())
                .data(commentPage.getContent().stream().map(commentMapper::commentToResponse).collect(Collectors.toList()))
                .errorDescription("")
                .build();
    }

    public CommonResponse<CommentResponse> editComment(long commentId, CommentRequest commentRequest) {
        Comment comment = getCommentById(commentId);
        comment.setCommentText(commentRequest.getCommentText());
        comment.setTime(LocalDateTime.now());
        return CommonResponse.<CommentResponse>builder()
                .error("success")
                .timestamp(System.currentTimeMillis())
                .data(commentMapper.commentToResponse(commentRepository.save(comment)))
                .errorDescription("")
                .build();
    }

    public CommonResponse<CommentResponse> changeCommentDeleteStatus(long commentId, boolean status) {
        Comment comment = getCommentById(commentId);
        comment.setIsDeleted(status);
        return CommonResponse.<CommentResponse>builder()
                .error("success")
                .timestamp(System.currentTimeMillis())
                .data(commentMapper.commentToResponse(commentRepository.save(comment)))
                .errorDescription("")
                .build();
    }

    @Named("getCommentById")
    public Comment getCommentById(long commentId) {
        return commentRepository.findById(commentId).orElse(null);
    }

    @Named("commentsToResponse")
    public List<CommentResponse> commentsToResponse(List<Comment> comments) {
        if (comments == null) {
            return new ArrayList<>();
        }
        comments.sort(Comparator.comparing(Comment::getTime));
        return comments.stream().map(comment -> {
            CommentResponse commentResponse = commentMapper.commentToResponse(comment);
            commentResponse.setEmbeddedComments(
                    comment.getEmbeddedComments().isEmpty() ?
                            new ArrayList<>() :
                            commentsToResponse(comment.getEmbeddedComments()));
            return commentResponse;
        }).collect(Collectors.toList());
    }
}
