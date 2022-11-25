package main.service;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import main.api.request.LikeRequest;
import main.model.entities.Comment;
import main.model.entities.Like;
import main.model.entities.Person;
import main.model.entities.Post;
import main.repository.CommentsRepository;
import main.repository.LikesRepository;
import main.repository.PostsRepository;
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
    private LikesRepository likesRepository;

    @MockBean
    private PostsRepository postsRepository;

    @MockBean
    private CommentsRepository commentsRepository;

    private Person person;
    private Post liked1;
    private Comment liked2;
    private final String type1 ="Post";
    private final String type2 ="Comment";
    private LikeRequest likeRequest1;
    private LikeRequest likeRequest2;
    private List<Like> likes;

    @BeforeEach
    void setUp() {
        person = new Person();
        person.setId(1L);
        liked1 = new Post();
        liked1.setId(11L);
        liked1.setAuthor(person);
        liked2 = new Comment();
        liked2.setId(12L);
        liked2.setAuthor(person);
        likeRequest1 = new LikeRequest();
        likeRequest2 = new LikeRequest();
        likeRequest1.setType(type1);
        likeRequest2.setType(type2);
        likes = new ArrayList<>();
    }

    @AfterEach
    void tearDown() {
        person = null;
        liked1 = null;
        liked2 = null;
        likeRequest1 = null;
        likeRequest2 = null;
        likes = null;
    }

    @Test
    void putLike() {
        when(likesRepository.save(any())).then(invocation -> {
            likes.add(invocation.getArgument(0));
            return null;
        });
        when(postsRepository.findById(any())).thenReturn(Optional.of(liked1));
        when(commentsRepository.findById(any())).thenReturn(Optional.of(liked2));
        when(likesRepository.findLikesByEntity(any(), any())).thenReturn(likes);
        when(personsService.getPersonByContext()).thenReturn(person);
        when(likesRepository.findLikeByPersonAndEntity(any(), any(), any())).thenReturn(Optional.empty());

        assertTrue(likesService.putLike(likeRequest1).getData().getUsers().contains(person.getId()));
        verify(likesRepository).save(any(Like.class));
        assertTrue(likesService.putLike(likeRequest2).getData().getUsers().contains(person.getId()));
    }

    @Test
    void getLikesResponse() {
        Like like = new Like();
        like.setPerson(person);
        likes.add(like);
        when(postsRepository.findById(any())).thenReturn(Optional.of(liked1));
        when(commentsRepository.findById(any())).thenReturn(Optional.of(liked2));
        when(likesRepository.findLikesByEntity(any(), any())).thenReturn(likes);

        assertTrue(likesService.getLikesResponse(anyLong(), type1).getData().getUsers().contains(person.getId()));
        assertTrue(likesService.getLikesResponse(anyLong(), type2).getData().getUsers().contains(person.getId()));
    }

    @Test
    void deleteLike() {
        Like like = new Like();
        like.setPerson(person);
        likes.add(like);
        when(personsService.getPersonByContext()).thenReturn(person);
        when(postsRepository.findById(any())).thenReturn(Optional.of(liked1));
        when(commentsRepository.findById(any())).thenReturn(Optional.of(liked2));
        when(likesRepository.findLikeByPersonAndEntity(any(), any(), any())).thenReturn(Optional.of(like));
        when(likesRepository.findLikesByEntity(any(), any())).thenReturn(likes);

        likesService.deleteLike(1L, type1);
        verify(personsService).getPersonByContext();
        verify(likesRepository).findLikeByPersonAndEntity(any(), any(), any());
        verify(likesRepository).delete(like);
    }

    @Test
    void getLikesCount() {
        int likesCount = 5;
        for (int i = 0; i < likesCount; i++) {
            Like like = new Like();
            like.setPerson(person);
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
        like.setPerson(person);
        when(personsService.getPersonByContext()).thenReturn(person);
        when(likesRepository.findLikeByPersonAndEntity(liked1.getType(), liked1, person)).thenReturn(Optional.empty());
        when(likesRepository.findLikeByPersonAndEntity(liked2.getType(), liked2, person)).thenReturn(Optional.of(like));
        assertFalse(likesService.getMyLike(liked1));
        verify(likesRepository).findLikeByPersonAndEntity(liked1.getType(), liked1, person);
        assertTrue(likesService.getMyLike(liked2));
    }
}