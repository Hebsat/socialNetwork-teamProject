package socialnet.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import socialnet.api.request.FindPostRq;
import socialnet.api.request.PostRq;
import socialnet.api.response.CommonRs;
import socialnet.api.response.PostRs;
import socialnet.errors.EmptyFieldException;
import socialnet.errors.PersonNotFoundException;
import socialnet.kafka.NotificationsKafkaProducer;
import socialnet.mappers.PostMapper;
import socialnet.model.entities.Friendship;
import socialnet.model.entities.Person;
import socialnet.model.entities.Post;
import socialnet.model.entities.Tag;
import socialnet.model.enums.FriendshipStatusTypes;
import socialnet.repository.FriendshipsRepository;
import socialnet.repository.PersonsRepository;
import socialnet.repository.PostsRepository;
import socialnet.service.search.SearchPosts;
import socialnet.service.util.NetworkPageRequest;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static socialnet.model.enums.FriendshipStatusTypes.BLOCKED;

@Service
@RequiredArgsConstructor
public class PostsService {

    private final PostsRepository postsRepository;
    private final PersonsRepository personsRepository;
    private final FriendshipsRepository friendshipsRepository;
    private final TagsService tagsService;
    private final NotificationsService notificationsService;
    private final PostMapper postMapper;
    private final SearchPosts searchPosts;
    private final PersonCacheService personCacheService;
    private final NotificationsKafkaProducer notificationsKafkaProducer;

    @Value("${socialNetwork.timezone}")
    private String timezone;

    public CommonRs<PostRs> createPost(PostRq postRq, long userId, Long timestamp) throws PersonNotFoundException {
        Person person = personsRepository.findById(userId).orElse(null);
        if (!validatePerson(person)) {
            throw new PersonNotFoundException("Invalid user id");
        }
        LocalDateTime postPublishingTime = timestamp == null ? LocalDateTime.now(ZoneId.of(timezone)) : new Timestamp(timestamp).toLocalDateTime();
        Post post = postMapper.postRequestToNewPost(postRq, person, postPublishingTime);
        PostRs postRs = postMapper.postToResponse(postsRepository.save(updateTagsInPost(new ArrayList<>(post.getTags()), post)));
        createNotifications(post, person);
        return buildCommonResponse(postRs);
    }

    public CommonRs<List<PostRs>> getFeeds(int offset, int size) {
        Pageable pageable = NetworkPageRequest.of(offset, size);
        Page<Post> postsWithBlockedPosts = blockPosts(pageable);
        Page<Post> postPage;
        if (!postsWithBlockedPosts.isEmpty()) {
            postPage = postsWithBlockedPosts;
        } else {
            postPage = postsRepository.findPostsByTimeBeforeAndIsDeletedFalseAndIsBlockedFalseOrderByTimeDesc(pageable, LocalDateTime.now(ZoneId.of(timezone)));
        }
        return buildCommonResponse(offset, size, postPage.getContent(), postPage.getTotalElements());
    }

    public CommonRs<List<PostRs>> getAllPostsByAuthor(int offset, int size, Person postsAuthor) {
        if (blockPostsByAuthor(postsAuthor)) {
            return buildCommonResponse(offset, size, new ArrayList<>(), 0L);
        }
        Pageable pageable = NetworkPageRequest.of(offset, size);
        Page<Post> postPage = postsRepository.findPostsByAuthorAndIsBlockedFalseOrderByTimeDesc(pageable, postsAuthor);
        return buildCommonResponse(offset, size, postPage.getContent(), postPage.getTotalElements());
    }

    public CommonRs<PostRs> getPostById(long postId) {
        PostRs postRs = postMapper.postToResponse(findPostById(postId));
        return buildCommonResponse(postRs);
    }

    public CommonRs<PostRs> updatePost(long postId, PostRq postRq) throws PersonNotFoundException {
        Post post = findPostById(postId);
        if (!validatePerson(post.getAuthor())) {
            throw new PersonNotFoundException("Invalid user id");
        }
        post.setTitle(postRq.getTitle());
        post.setPostText(postRq.getPostText());
        List<Tag> tags = tagsService.stringsToTagsMapper(postRq.getTags());
        Post newPost = updateTagsInPost(tags, post);
        PostRs postRs = postMapper.postToResponse(postsRepository.save(newPost));
        return buildCommonResponse(postRs);
    }

    public CommonRs<PostRs> changeDeleteStatusInPost(long postId, boolean deleteStatus) throws PersonNotFoundException {
        Post post = findPostById(postId);
        if (!validatePerson(post.getAuthor())) {
            throw new PersonNotFoundException("Invalid user id");
        }
        post.setIsDeleted(deleteStatus);
        post.setTimeDelete(LocalDateTime.now(ZoneId.of(timezone)));
        PostRs postRs = postMapper.postToResponse(postsRepository.save(post));
        return buildCommonResponse(postRs);
    }

    public Post findPostById(long postId) {
        return postsRepository.findById(postId).orElse(null);
    }

    private Post updateTagsInPost(List<Tag> tags, Post post) {
        if (post.getTags() != null) {
            List<Tag> oldTags = post.getTags();
            oldTags.removeAll(tags);
            oldTags.forEach(tag -> tagsService.dropPostFromTag(tag, post));
        }
        tags.forEach(tag -> tagsService.addPostToTag(tag, post));
        post.setTags(tags);
        return post;
    }

    private List<PostRs> postsToResponse(List<Post> posts) {
        return posts.stream().map(postMapper::postToResponse).collect(Collectors.toList());
    }

    private boolean validatePerson(Person person) {
        return person != null && person.equals(personCacheService.getPersonByContext());
    }

    public CommonRs<List<PostRs>> findPosts(FindPostRq postRq, int offset, int perPage) throws EmptyFieldException {
        if (postRq.getText() == null) {
            throw new EmptyFieldException("Field 'text' is required but empty");
        }
        Page<Post> posts = searchPosts.findPosts(postRq, offset, perPage);
        return buildCommonResponse(offset, perPage, posts.getContent(), posts.getTotalElements());
    }

    private CommonRs<List<PostRs>> buildCommonResponse(int offset, int perPage, List<Post> posts, Long total) {
        return CommonRs.<List<PostRs>>builder()
                .timestamp(System.currentTimeMillis())
                .offset(offset)
                .perPage(perPage)
                .total(total)
                .data(postsToResponse(posts))
                .build();
    }

    private CommonRs<PostRs> buildCommonResponse(PostRs postRs) {
        return CommonRs.<PostRs>builder()
                .timestamp(System.currentTimeMillis())
                .data(postRs)
                .build();
    }

    private void createNotifications(Post post, Person person) {
        friendshipsRepository.findFriendshipsByDstPerson(person).forEach(friendship -> {
            if (friendship.getFriendshipStatus().equals(FriendshipStatusTypes.FRIEND) ||
                    friendship.getFriendshipStatus().equals(FriendshipStatusTypes.SUBSCRIBED)) {
                if (friendship.getSrcPerson().getPersonSettings().getPostNotification()) {
                    notificationsService.sendNotificationToWs(post, friendship.getSrcPerson());
                    notificationsKafkaProducer.sendMessage(post, friendship.getSrcPerson());
                    notificationsService.sendNotificationToTelegramBot(post, friendship.getSrcPerson());
                }
            }
        });
    }

    private Page<Post> blockPosts(Pageable pageable) {
        Person person = personCacheService.getPersonByEmail(SecurityContextHolder.getContext().getAuthentication().getName());
        List<Friendship> friendships = friendshipsRepository.findFriendshipsByDstPersonIdAndFriendshipStatus(person.getId(), BLOCKED);
        if (friendships.size() != 0) {
            List<Person> srcPersons = new ArrayList<>();
            friendships.forEach(friendship -> srcPersons.add(friendship.getSrcPerson()));
            return postsRepository.findPostsByAuthorIsNotInAndIsDeletedFalseOrderByTimeDesc(pageable, srcPersons);
        } else {
            return Page.empty();
        }
    }

    private boolean blockPostsByAuthor(Person author) {
        Person me = personCacheService.getPersonByContext();
        Optional<Friendship> friendship = friendshipsRepository.findFriendshipByFriendshipStatusAndSrcPersonIdAndDstPersonId(BLOCKED, author.getId(), me.getId());
        return friendship.isPresent();
    }
}
