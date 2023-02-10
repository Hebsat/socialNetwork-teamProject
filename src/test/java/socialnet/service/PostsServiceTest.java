package socialnet.service;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import socialnet.api.request.FindPostRq;
import socialnet.api.request.PostRq;
import socialnet.api.response.PersonRs;
import socialnet.errors.EmptyFieldException;
import socialnet.errors.PersonNotFoundException;
import socialnet.kafka.NotificationsKafkaProducer;
import socialnet.mappers.PersonMapper;
import socialnet.model.entities.*;
import socialnet.model.enums.FriendshipStatusTypes;
import socialnet.repository.FriendshipsRepository;
import socialnet.repository.PersonsRepository;
import socialnet.repository.PostsRepository;
import socialnet.service.search.SearchPosts;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureEmbeddedDatabase
class PostsServiceTest {

    @Autowired
    private PostsService postsService;

    @MockBean
    private PostsRepository postsRepository;
    @MockBean
    private PersonsRepository personsRepository;
    @MockBean
    private FriendshipsRepository friendshipsRepository;
    @MockBean
    private TagsService tagsService;
    @MockBean
    private LikesService likesService;
    @MockBean
    private CommentsService commentsService;
    @MockBean
    private NotificationsKafkaProducer notificationsKafkaProducer;
    @MockBean
    private PersonMapper personMapper;
    @MockBean
    private Authentication authentication;

    @MockBean
    private SearchPosts searchPosts;

    private PostRq postRq;
    private Person person;
    private Post post;
    private Friendship friendship;
    private PersonSettings personSettings;
    private final String postTitle = "postTitle";
    private final String postText = "postText";
    private final int offset = 0;
    private final int size = 20;

    @BeforeEach
    void setUp() {
        when(authentication.getName()).thenReturn(null);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        friendship = new Friendship();
        friendship.setFriendshipStatus(FriendshipStatusTypes.FRIEND);
        friendship.setSentTime(LocalDateTime.now());
        postRq = new PostRq();
        postRq.setTitle(postTitle);
        postRq.setPostText(postText);
        person = new Person();
        person.setId(1L);
        post = new Post();
        post.setAuthor(person);
        post.setPostText(postText);
        post.setTitle(postTitle);
        post.setIsDeleted(false);
        post.setIsBlocked(false);
        post.setTime(LocalDateTime.now());
        personSettings = new PersonSettings();
        personSettings.setPostNotification(true);
        person.setPersonSettings(personSettings);
        friendship.setSrcPerson(person);
        friendship.setDstPerson(person);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        postRq = null;
        person = null;
        post = null;
        friendship = null;
        personSettings = null;
    }

    @Test
    void createPost() throws PersonNotFoundException {
        when(personsRepository.findById(anyLong())).thenReturn(Optional.of(person));
        when(personsRepository.findPersonByEmail(any())).thenReturn(Optional.of(person));
        when(tagsService.tagsToStringsMapper(any())).thenReturn(new ArrayList<>());
        when(tagsService.stringsToTagsMapper(any())).thenReturn(new ArrayList<>());
        when(likesService.getLikesCount(any())).thenReturn(0);
        when(likesService.getMyLike(any())).thenReturn(false);
        when(commentsService.embeddedCommentsToResponse(any())).thenReturn(new ArrayList<>());
        when(personMapper.toPersonResponse(any())).thenReturn(PersonRs.builder().id(1L).build());
        when(postsRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(friendshipsRepository.findFriendshipsByDstPerson(any())).thenReturn(List.of(friendship));

        assertEquals(postText, postsService.createPost(postRq, 1L, null).getData().getPostText());
        verify(notificationsKafkaProducer).sendMessage(any(), any());
    }

    @Test
    void getFeeds() {
        when(personsRepository.findPersonByEmail(any())).thenReturn(Optional.of(person));
        when(postsRepository.findPostsByTimeBeforeAndIsDeletedFalseAndIsBlockedFalseOrderByTimeDesc(any(), any())).thenReturn(new PageImpl<>(List.of(post)));
        when(tagsService.tagsToStringsMapper(any())).thenReturn(new ArrayList<>());
        when(tagsService.stringsToTagsMapper(any())).thenReturn(new ArrayList<>());
        when(likesService.getLikesCount(any())).thenReturn(0);
        when(likesService.getMyLike(any())).thenReturn(false);
        when(commentsService.embeddedCommentsToResponse(any())).thenReturn(new ArrayList<>());
        when(personMapper.toPersonResponse(any())).thenReturn(PersonRs.builder().id(1L).build());

        assertEquals(postText, postsService.getFeeds(offset, size).getData().get(0).getPostText());
    }

    @Test
    void getAllPostsByAuthor() {
        when(postsRepository.findPostsByAuthorAndIsBlockedFalseOrderByTimeDesc(any(), any())).thenReturn(new PageImpl<>(List.of(post)));
        when(tagsService.tagsToStringsMapper(any())).thenReturn(new ArrayList<>());
        when(tagsService.stringsToTagsMapper(any())).thenReturn(new ArrayList<>());
        when(likesService.getLikesCount(any())).thenReturn(0);
        when(likesService.getMyLike(any())).thenReturn(false);
        when(commentsService.embeddedCommentsToResponse(any())).thenReturn(new ArrayList<>());
        when(personMapper.toPersonResponse(any())).thenReturn(PersonRs.builder().id(1L).build());
        when(personsRepository.findPersonByEmail(any())).thenReturn(Optional.of(person));
        when(friendshipsRepository.findFriendshipByFriendshipStatusAndSrcPersonIdAndDstPersonId(any(), any(), any())).thenReturn(Optional.empty());

        assertEquals(postText, postsService.getAllPostsByAuthor(offset, size, person).getData().get(0).getPostText());
    }

    @Test
    void getPostById() {
        when(postsRepository.findById(anyLong())).thenReturn(Optional.of(post));
        when(tagsService.tagsToStringsMapper(any())).thenReturn(new ArrayList<>());
        when(tagsService.stringsToTagsMapper(any())).thenReturn(new ArrayList<>());
        when(likesService.getLikesCount(any())).thenReturn(0);
        when(likesService.getMyLike(any())).thenReturn(false);
        when(commentsService.embeddedCommentsToResponse(any())).thenReturn(new ArrayList<>());
        when(personMapper.toPersonResponse(any())).thenReturn(PersonRs.builder().id(1L).build());

        assertEquals(postText, postsService.getPostById(anyLong()).getData().getPostText());
    }

    @Test
    void updatePost() throws PersonNotFoundException {
        post.setPostText("errorText");
        when(personsRepository.findPersonByEmail(any())).thenReturn(Optional.of(person));
        when(postsRepository.findById(anyLong())).thenReturn(Optional.of(post));
        when(tagsService.tagsToStringsMapper(any())).thenReturn(new ArrayList<>());
        when(tagsService.stringsToTagsMapper(any())).thenReturn(new ArrayList<>());
        when(likesService.getLikesCount(any())).thenReturn(0);
        when(likesService.getMyLike(any())).thenReturn(false);
        when(commentsService.embeddedCommentsToResponse(any())).thenReturn(new ArrayList<>());
        when(personMapper.toPersonResponse(any())).thenReturn(PersonRs.builder().id(1L).build());
        when(postsRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertEquals(postText, postsService.updatePost(anyLong(), postRq).getData().getPostText());
    }

    @Test
    void changeDeleteStatusInPost() throws PersonNotFoundException {
        AtomicReference<Post> postToDelete = new AtomicReference<>();
        when(personsRepository.findPersonByEmail(any())).thenReturn(Optional.of(person));
        when(postsRepository.findById(anyLong())).thenReturn(Optional.of(post));
        when(tagsService.tagsToStringsMapper(any())).thenReturn(new ArrayList<>());
        when(tagsService.stringsToTagsMapper(any())).thenReturn(new ArrayList<>());
        when(likesService.getLikesCount(any())).thenReturn(0);
        when(likesService.getMyLike(any())).thenReturn(false);
        when(commentsService.embeddedCommentsToResponse(any())).thenReturn(new ArrayList<>());
        when(personMapper.toPersonResponse(any())).thenReturn(PersonRs.builder().id(1L).build());
        when(postsRepository.save(any())).then(invocation -> {
            Post savedPost = invocation.getArgument(0);
            postToDelete.set(savedPost);
            return savedPost;
        });

        postsService.changeDeleteStatusInPost(anyLong(), true);
        assertTrue(postToDelete.get().getIsDeleted());
    }

    @Test
    void findPostById() {
        postsService.getPostById(anyLong());

        verify(postsRepository).findById(anyLong());
    }

    @Test
    void findPosts() throws EmptyFieldException {
        FindPostRq postRq = new FindPostRq();
        postRq.setText("some text");
        when(searchPosts.findPosts(any(), anyInt(), anyInt())).thenReturn(Page.empty());
        postsService.findPosts(postRq, 0, 20);
        verify(searchPosts, times(1)).findPosts(postRq, 0, 20);
    }

    @Test
    void findPostException() {
        FindPostRq postRq = new FindPostRq();
        Throwable thrown = catchThrowable(() -> postsService.findPosts(postRq, 0, 20));
        assertThat(thrown).isInstanceOf(EmptyFieldException.class);
        assertThat(thrown.getMessage()).isNotBlank();
    }
}