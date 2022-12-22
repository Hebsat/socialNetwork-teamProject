package soialNetworkApp.service;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import soialNetworkApp.api.response.PersonResponse;
import soialNetworkApp.mappers.PersonMapper;
import soialNetworkApp.model.entities.*;
import soialNetworkApp.model.enums.FriendshipStatusTypes;
import soialNetworkApp.model.enums.NotificationTypes;
import soialNetworkApp.repository.FriendshipsRepository;
import soialNetworkApp.repository.NotificationsRepository;
import soialNetworkApp.repository.PersonsRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureEmbeddedDatabase
class NotificationsServiceTest {

    @Autowired
    private NotificationsService notificationsService;

    @MockBean
    private Authentication authentication;
    @MockBean
    private PersonsRepository personsRepository;
    @MockBean
    private NotificationsRepository notificationsRepository;
    @MockBean
    private FriendshipsRepository friendshipsRepository;
    @MockBean
    private PersonMapper personMapper;
    @MockBean
    private SimpMessagingTemplate template;

    private Person person;
    private Notification notification1;
    private Notification notification2;
    private Post post;
    private PersonSettings personSettings;
    private Friendship friendship;
    private FriendshipStatus friendshipStatus;
    private final int offset = 0;
    private final int size = 10;

    @BeforeEach
    void setUp() {
        when(authentication.getName()).thenReturn(null);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        person = new Person();
        person.setId(1L);
        post = new Post();
        post.setAuthor(person);
        notification1 = new Notification();
        notification2 = new Notification();
        notification1.setId(1L);
        notification1.setId(2L);
        notification1.setEntity(post);
        notification2.setEntity(post);
        notification1.setIsRead(false);
        notification2.setIsRead(false);
        notification1.setPerson(person);
        notification2.setPerson(person);
        personSettings = new PersonSettings();
        personSettings.setFriendBirthdayNotification(true);
        person.setPersonSettings(personSettings);
        friendship = new Friendship();
        friendshipStatus = new FriendshipStatus();
        friendshipStatus.setCode(FriendshipStatusTypes.FRIEND);
        friendship.setSrcPerson(person);
        friendship.setDstPerson(person);
        friendship.setFriendshipStatus(friendshipStatus);
    }

    @AfterEach
    void tearDown() {
        person = null;
        notification1 = null;
        notification2 = null;
        post = null;
        personSettings = null;
        friendshipStatus = null;
        friendship = null;
    }

    @Test
    void getAllNotificationsByPerson() {
        when(personsRepository.findPersonByEmail(any())).thenReturn(Optional.of(person));
        when(notificationsRepository.findAllByPersonAndIsReadIsFalse(any(), any())).thenReturn(new PageImpl<>(List.of(notification1)));
        when(personMapper.toPersonResponse(any())).thenReturn(PersonResponse.builder().id(1L).build());

        assertEquals(notificationsService.getAllNotificationsByPerson(offset, size).getData().stream().findFirst().get().getNotificationType(), NotificationTypes.POST);
        assertEquals(notificationsService.getAllNotificationsByPerson(offset, size).getData().stream().findFirst().get().getEntityAuthor().getId(), 1L);
    }

    @Test
    void markAllNotificationsStatusAsRead() {
        List<Notification> notifications = new ArrayList<>();
        when(personsRepository.findPersonByEmail(any())).thenReturn(Optional.of(person));
        when(notificationsRepository.findAllByPersonAndIsReadIsFalse(any(), any())).thenReturn(new PageImpl<>(List.of(notification1, notification2)));
        when(notificationsRepository.findAllByPersonAndIsReadIsFalse(any())).thenReturn(List.of(notification1, notification2));
        when(personMapper.toPersonResponse(any())).thenReturn(PersonResponse.builder().id(1L).build());
        when(notificationsRepository.save(any())).then(invocation -> {
            Notification notification = invocation.getArgument(0);
            notifications.add(notification);
            return notification;
        });

        notificationsService.markNotificationStatusAsRead(null, true);
        verify(notificationsRepository, Mockito.times(2)).save(any());
        assertEquals(2, notifications.size());
        assertTrue(notifications.stream().map(Notification::getIsRead).allMatch(aBoolean -> aBoolean.equals(true)));
    }

    @Test
    void markNotificationStatusAsRead() {
        List<Notification> notifications = new ArrayList<>();
        when(personsRepository.findPersonByEmail(any())).thenReturn(Optional.of(person));
        when(notificationsRepository.findAllByPersonAndIsReadIsFalse(any(), any())).thenReturn(new PageImpl<>(List.of(notification1, notification2)));
        when(notificationsRepository.findAllByPersonAndIsReadIsFalse(any())).thenReturn(List.of(notification1, notification2));
        when(notificationsRepository.findById(any())).thenReturn(Optional.of(notification1));
        when(personMapper.toPersonResponse(any())).thenReturn(PersonResponse.builder().id(1L).build());
        when(notificationsRepository.save(any())).then(invocation -> {
            Notification notification = invocation.getArgument(0);
            notifications.add(notification);
            return notification;
        });

        notificationsService.markNotificationStatusAsRead(1L, false);
        verify(notificationsRepository).save(any());
        assertEquals(1, notifications.size());
        assertTrue(notifications.get(0).getIsRead());
    }

    @Test
    void createNotification() {
        when(notificationsRepository.findAllByPersonAndIsReadIsFalse(any(), any())).thenReturn(new PageImpl<>(new ArrayList<>()));

        notificationsService.createNotification(post, person);
        verify(notificationsRepository).save(any());
        verify(template).convertAndSend(anyString(), (Object) any());
    }

    @Test
    void birthdayNotificator() {
        when(personsRepository.findPeopleByBirthDate(anyInt(), anyInt())).thenReturn(List.of(person));
        when(friendshipsRepository.findFriendshipsByDstPerson(any())).thenReturn(List.of(friendship));
        when(notificationsRepository.findAllByPersonAndIsReadIsFalse(any(), any())).thenReturn(new PageImpl<>(new ArrayList<>()));

        notificationsService.birthdaysNotificator();
        verify(notificationsRepository).save(any());
    }

    @Test
    void deleteNotification() {
        when(notificationsRepository.findNotificationByEntity(any(), any())).thenReturn(Optional.of(new Notification()));

        notificationsService.deleteNotification(post);
        verify(notificationsRepository).delete(any());
    }
}