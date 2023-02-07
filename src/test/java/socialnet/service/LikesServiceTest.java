package socialnet.service;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import socialnet.api.request.LikeRq;
import socialnet.errors.NotFoundException;
import socialnet.model.entities.*;
import socialnet.repository.CommentsRepository;
import socialnet.repository.LikesRepository;
import socialnet.repository.PostsRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureEmbeddedDatabase
class LikesServiceTest {

    @Autowired
    private LikesService likesService;

    @MockBean
    private PersonsService personsService;

    @MockBean
    private PersonCacheService personCacheService;

    @MockBean
    private LikesRepository likesRepository;

    @MockBean
    private PostsRepository postsRepository;

    @MockBean
    private CommentsRepository commentsRepository;

    @MockBean
    private NotificationsService notificationsService;

    private Person person;
    private Post liked1;
    private Comment liked2;
    private final String type1 ="Post";
    private final String type2 ="Comment";
    private LikeRq likeRq1;
    private LikeRq likeRq2;
    private List<Like> likes;

    @BeforeEach
    void setUp() {
        person = new Person();
        person.setId(1L);
        PersonSettings settings = new PersonSettings();
        settings.setLikeNotification(true);
        person.setPersonSettings(settings);
        liked1 = new Post();
        liked1.setId(11L);
        liked1.setAuthor(person);
        liked2 = new Comment();
        liked2.setId(12L);
        liked2.setAuthor(person);
        likeRq1 = new LikeRq();
        likeRq2 = new LikeRq();
        likeRq1.setType(type1);
        likeRq2.setType(type2);
        likes = new ArrayList<>();
    }

    @AfterEach
    void tearDown() {
        person = null;
        liked1 = null;
        liked2 = null;
        likeRq1 = null;
        likeRq2 = null;
        likes = null;
    }

    @Test
    void putLike() throws Exception {
        when(likesRepository.save(any())).then(invocation -> {
            likes.add(invocation.getArgument(0));
            return null;
        });
        when(postsRepository.findById(any())).thenReturn(Optional.of(liked1));
        when(commentsRepository.findById(any())).thenReturn(Optional.of(liked2));
        when(likesRepository.findLikesByEntity(any(), any())).thenReturn(likes);
        when(personCacheService.getPersonByContext()).thenReturn(person);
        when(likesRepository.findLikeByPersonAndEntity(any(), any(), any())).thenReturn(Optional.empty());

        assertTrue(likesService.putLike(likeRq1).getData().getUsers().contains(person.getId()));
        verify(likesRepository).save(any(Like.class));
        assertTrue(likesService.putLike(likeRq2).getData().getUsers().contains(person.getId()));
    }

    @Test
    void getLikesResponse() throws NotFoundException {
        Like like = new Like();
        like.setAuthor(person);
        likes.add(like);
        when(postsRepository.findById(any())).thenReturn(Optional.of(liked1));
        when(commentsRepository.findById(any())).thenReturn(Optional.of(liked2));
        when(likesRepository.findLikesByEntity(any(), any())).thenReturn(likes);

        assertTrue(likesService.getLikesResponse(anyLong(), type1).getData().getUsers().contains(person.getId()));
        assertTrue(likesService.getLikesResponse(anyLong(), type2).getData().getUsers().contains(person.getId()));
    }

    @Test
    void deleteLike() throws Exception {
        Like like = new Like();
        like.setAuthor(person);
        likes.add(like);
        when(personCacheService.getPersonByContext()).thenReturn(person);
        when(postsRepository.findById(any())).thenReturn(Optional.of(liked1));
        when(commentsRepository.findById(any())).thenReturn(Optional.of(liked2));
        when(likesRepository.findLikeByPersonAndEntity(any(), any(), any())).thenReturn(Optional.of(like));
        when(likesRepository.findLikesByEntity(any(), any())).thenReturn(likes);

        likesService.deleteLike(1L, type1);
        verify(personCacheService).getPersonByContext();
        verify(likesRepository).findLikeByPersonAndEntity(any(), any(), any());
        verify(likesRepository).delete(like);
    }

    @Test
    void getLikesCount() {
        int likesCount = 5;
        for (int i = 0; i < likesCount; i++) {
            Like like = new Like();
            like.setAuthor(person);
            likes.add(like);
        }
        when(likesRepository.findLikesByEntity(liked2.getType(), liked2)).thenReturn(likes);

        likesService.getLikesCount(liked1);
        verify(likesRepository).findLikesByEntity(liked1.getType(), liked1);
        assertEquals(likesCount, (int) likesService.getLikesCount(liked2));
        verify(likesRepository).findLikesByEntity(liked2.getType(), liked2);
    }

    @Test
    void getMyLike() {
        Like like = new Like();
        like.setAuthor(person);
        when(personCacheService.getPersonByContext()).thenReturn(person);
        when(likesRepository.findLikeByPersonAndEntity(liked1.getType(), liked1, person)).thenReturn(Optional.empty());
        when(likesRepository.findLikeByPersonAndEntity(liked2.getType(), liked2, person)).thenReturn(Optional.of(like));

        assertFalse(likesService.getMyLike(liked1));
        verify(likesRepository).findLikeByPersonAndEntity(liked1.getType(), liked1, person);
        assertTrue(likesService.getMyLike(liked2));
    }
}