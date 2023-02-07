package socialnet.service;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.checkerframework.checker.units.qual.A;
import socialnet.api.response.CommonRs;
import socialnet.api.response.PersonRs;
import socialnet.kafka.NotificationsKafkaProducer;
import socialnet.mappers.PersonMapper;
import socialnet.model.entities.Friendship;
import socialnet.model.entities.Person;
import socialnet.model.entities.PersonSettings;
import socialnet.model.enums.FriendshipStatusTypes;
import socialnet.repository.FriendshipsRepository;
import socialnet.repository.PersonsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static socialnet.model.enums.FriendshipStatusTypes.*;
import static org.mockito.Mockito.*;
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureEmbeddedDatabase
class FriendsServiceTest {

    @Autowired
    private FriendsService friendsService;

    @MockBean
    private NotificationsKafkaProducer notificationsKafkaProducer;

    @MockBean
    private FriendshipsRepository friendshipsRepository;

    @MockBean
    private PersonsRepository personsRepository;

    @MockBean
    private PersonMapper personMapper;

    @MockBean
    private PersonCacheService personCacheService;

    private Friendship fsCurPsRcFr;
    private Friendship fsRcFr;
    private Friendship fsCurPsFr;
    private Friendship fsFr;
    private Friendship fsCurPsRqFr;
    private Friendship fsRqFr;

    private final static String CURRENT_PERSON_MAIL = "rhoncus.nullam@yahoo.edu";
    private final static String C_FRIEND_MAIL = "molestie@yahoo.edu";
    private final static String RECEIVED_FRIEND_MAIL = "nostra.per.inceptos@hotmail.com";
    private final static String REQUESTED_FRIEND_MAIL = "urna.suscipit@outlook.com";
    private final static String UNKNOWN_PERSON_MAIL = "blandit.congue@hotmail.couk";

    private final static Person CURRENT_PERSON = new Person(1L, CURRENT_PERSON_MAIL);
    private final static Person C_FRIEND = new Person(2L, C_FRIEND_MAIL);
    private final static Person RECEIVED_FRIEND = new Person(3L, RECEIVED_FRIEND_MAIL);
    private final static Person REQUESTED_FRIEND = new Person(4L, REQUESTED_FRIEND_MAIL);
    private final static Person UNKNOWN_PERSON = new Person(5L, UNKNOWN_PERSON_MAIL);

    private final static PersonRs C_FRIEND_DTO = PersonRs.builder().id(2L).email(C_FRIEND_MAIL).build();
    private final static PersonRs RECEIVED_FRIEND_DTO = PersonRs.builder().id(3L).email(RECEIVED_FRIEND_MAIL).build();
    private final static PersonRs REQUESTED_FRIEND_DTO = PersonRs.builder().id(4L).email(REQUESTED_FRIEND_MAIL).build();

    private static final LocalDateTime TIME = LocalDateTime.now();
    private static final Integer OFFSET = 0;
    private static final Integer SIZE = 5;
    private static final Pageable PAGEABLE = PageRequest.of(OFFSET, SIZE);
    private static final ArrayList<Person> FRIENDS = new ArrayList<>(Arrays.asList(C_FRIEND));
    private static final Page<Person> PAGE_FRIENDS = new PageImpl<>(FRIENDS, PAGEABLE, FRIENDS.size());
    private static final ArrayList<Person> RECEIVED_FRIENDS = new ArrayList<>(Arrays.asList(RECEIVED_FRIEND));
    private static final ArrayList<Person> REQUESTED_FRIENDS = new ArrayList<>(Arrays.asList(REQUESTED_FRIEND));
    private static final Page<Person> PAGE_RECEIVED_FRIENDS = new PageImpl<>(RECEIVED_FRIENDS, PAGEABLE, RECEIVED_FRIENDS.size());
    private static final Page<Person> PAGE_REQUESTED_FRIENDS = new PageImpl<>(REQUESTED_FRIENDS, PAGEABLE, REQUESTED_FRIENDS.size());

    private PersonSettings personSettings;

    @BeforeEach
    void setUp() {
        mockSecurityContext();
        buildFriendObjects();
        buildReceivedRequestObjects();
        buildRequestReceivedObjects();
        mockPersonsRepository();
        mockFriendshipsRepository();
        mockPersonMapper();
        personSettings = new PersonSettings();
        personSettings.setFriendRequestNotification(true);
        REQUESTED_FRIEND.setPersonSettings(personSettings);
    }

    private void mockSecurityContext(){
        Authentication authentication = Mockito.mock(Authentication.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication().getName()).thenReturn(CURRENT_PERSON_MAIL);
        when(personCacheService.getPersonByContext()).thenReturn(CURRENT_PERSON);
    }

    private void buildFriendObjects() {  //testDeleteFriend  // getFriends
        fsCurPsFr = new Friendship(1L, TIME, CURRENT_PERSON, C_FRIEND, FRIEND);
        fsFr = new Friendship(2L, TIME, C_FRIEND, CURRENT_PERSON, FRIEND);
    }

    private void buildReceivedRequestObjects() { //testAddFriend   //getRequests
        fsCurPsRcFr = new Friendship(3L, TIME, CURRENT_PERSON, RECEIVED_FRIEND, RECEIVED_REQUEST);
        fsRcFr = new Friendship(4L, TIME, RECEIVED_FRIEND, CURRENT_PERSON, REQUEST);
    }

    private void buildRequestReceivedObjects() { //testDeleteSentRequest
        fsCurPsRqFr = new Friendship(5L, TIME, CURRENT_PERSON, REQUESTED_FRIEND, REQUEST);
        fsRqFr = new Friendship(6L, TIME, REQUESTED_FRIEND, CURRENT_PERSON, RECEIVED_REQUEST);
    }

    private void mockPersonsRepository() {
        when(personsRepository.findPersonByEmail(CURRENT_PERSON_MAIL)).thenReturn(Optional.of(CURRENT_PERSON));
        when(personsRepository.findPersonById(CURRENT_PERSON.getId())).thenReturn(Optional.of(CURRENT_PERSON));
        when(personsRepository.findPersonById(RECEIVED_FRIEND.getId())).thenReturn(Optional.of(RECEIVED_FRIEND));
        when(personsRepository.findPersonById(C_FRIEND.getId())).thenReturn(Optional.of(C_FRIEND));
        when(personsRepository.findPersonById(REQUESTED_FRIEND.getId())).thenReturn(Optional.of(REQUESTED_FRIEND));
        when(personsRepository.findPersonById(UNKNOWN_PERSON.getId())).thenReturn(Optional.of(UNKNOWN_PERSON));
        when(personsRepository.findPersonByIdIn(List.of(C_FRIEND.getId()), PAGEABLE)).thenReturn(PAGE_FRIENDS);
        when(personsRepository.findPersonByIdIn(List.of(RECEIVED_FRIEND.getId()), PAGEABLE)).thenReturn(PAGE_RECEIVED_FRIENDS);
        when(personsRepository.findPersonByIdIn(List.of(REQUESTED_FRIEND.getId()), PAGEABLE)).thenReturn(PAGE_REQUESTED_FRIENDS);
    }

    private void mockFriendshipsRepository() {
        when(friendshipsRepository.findFriendshipBySrcPerson(CURRENT_PERSON)).thenReturn(List.of(fsCurPsFr, fsCurPsRcFr, fsCurPsRqFr));
        when(friendshipsRepository.findFriendshipBySrcPerson(RECEIVED_FRIEND)).thenReturn(List.of(fsRcFr));
        when(friendshipsRepository.findFriendshipBySrcPerson(C_FRIEND)).thenReturn(List.of(fsFr));
        when(friendshipsRepository.findFriendshipBySrcPerson(REQUESTED_FRIEND)).thenReturn(List.of(fsRqFr));

        when(friendshipsRepository.findFriendshipBySrcPersonAndDstPerson(C_FRIEND, CURRENT_PERSON)).thenReturn(Optional.of(fsCurPsFr));
        when(friendshipsRepository.findFriendshipBySrcPersonAndDstPerson(CURRENT_PERSON,C_FRIEND)).thenReturn(Optional.of(fsCurPsFr));

        when(friendshipsRepository.findFriendshipBySrcPersonIdAndDstPersonId(C_FRIEND.getId(), CURRENT_PERSON.getId())).thenReturn(Optional.of(fsCurPsFr));
        when(friendshipsRepository.findFriendshipBySrcPersonIdAndDstPersonId(CURRENT_PERSON.getId(), C_FRIEND.getId())).thenReturn(Optional.of(fsCurPsFr));

        when(friendshipsRepository.findFriendshipBySrcPersonAndDstPerson(CURRENT_PERSON, RECEIVED_FRIEND)).thenReturn(Optional.of(fsCurPsRcFr));
        when(friendshipsRepository.findFriendshipBySrcPersonAndDstPerson(RECEIVED_FRIEND, CURRENT_PERSON)).thenReturn(Optional.of(fsRcFr));

        when(friendshipsRepository.findFriendshipBySrcPersonAndDstPerson(CURRENT_PERSON, REQUESTED_FRIEND)).thenReturn(Optional.of(fsCurPsRqFr));
        when(friendshipsRepository.findFriendshipBySrcPersonAndDstPerson(REQUESTED_FRIEND, CURRENT_PERSON)).thenReturn(Optional.of(fsRqFr));
    }

    private void mockPersonMapper() {
        when(personMapper.toPersonResponse(C_FRIEND)).thenReturn(C_FRIEND_DTO);
        when(personMapper.toPersonResponse(RECEIVED_FRIEND)).thenReturn(RECEIVED_FRIEND_DTO);
        when(personMapper.toPersonResponse(REQUESTED_FRIEND)).thenReturn(REQUESTED_FRIEND_DTO);
    }

    @Test
    void getOutgoingRequest(){
        CommonRs<List<PersonRs>> resFriends = friendsService.getOutgoingRequests(OFFSET, SIZE);
        PersonRs resDto = resFriends.getData().get(0);
        assertEquals(REQUESTED_FRIENDS.size(), resFriends.getData().size());
        assertEquals(REQUESTED_FRIEND.getId(), resDto.getId());
        assertEquals(REQUESTED_FRIEND.getEmail(), resDto.getEmail());
    }

    @Test
    void userBlockUser() throws Exception {
        friendsService.userBlocksUser(C_FRIEND.getId());
        verify(friendshipsRepository, times(1)).delete(any());
        verify(friendshipsRepository, times(1)).save(any());
        assertEquals(BLOCKED, fsCurPsFr.getFriendshipStatus());
    }

    @Test
    void addFriend() throws Exception {
        friendsService.addFriend(RECEIVED_FRIEND.getId());
        assertEquals(FRIEND, fsCurPsRcFr.getFriendshipStatus());
        assertEquals(FRIEND, fsRcFr.getFriendshipStatus());
    }

    @Test
    void sendFriendshipRequest() throws Exception {
        friendsService.sendFriendshipRequest(REQUESTED_FRIEND.getId());
        verify(friendshipsRepository, times(2)).save(any());
        verify(notificationsKafkaProducer, times(1)).sendMessage(any(), any());
    }

    @Test
    void deleteFriend() throws Exception {
        friendsService.deleteFriend(C_FRIEND.getId());
        verify(friendshipsRepository, times(2)).delete(any());
    }

    @Test
    void deleteSentFriendshipRequest() throws Exception {
        friendsService.deleteSentFriendshipRequest(REQUESTED_FRIEND.getId());
        verify(friendshipsRepository, times(2)).delete(any());
    }

    @Test
    void getFriends()  {
        CommonRs<List<PersonRs>> resFriends = friendsService.getFriends(OFFSET, SIZE);
        PersonRs resDto = resFriends.getData().get(0);
        assertEquals(FRIENDS.size(), resFriends.getData().size());
        assertEquals(C_FRIEND.getId(), resDto.getId());
        assertEquals(C_FRIEND.getEmail(), resDto.getEmail());
    }

    @Test
    void getRequestedPersons() throws Exception {
        CommonRs<List<PersonRs>> resReceivedFriends = friendsService.getRequestedPersons(OFFSET, SIZE);
        PersonRs resDto = resReceivedFriends.getData().get(0);
        assertEquals(RECEIVED_FRIENDS.size(), resReceivedFriends.getData().size());
        assertEquals(RECEIVED_FRIEND.getId(), resDto.getId());
        assertEquals(RECEIVED_FRIEND.getEmail(), resDto.getEmail());
    }

    @Test
    void getStatusTwoPersons() {
        FriendshipStatusTypes actualFriendshipStatusTypes = friendsService.getStatusTwoPersons(CURRENT_PERSON, C_FRIEND);
        assertEquals(FRIEND, actualFriendshipStatusTypes);
    }

    @Test
    void getFriendsRecommendation(){
        Person person = new Person();
        when(personsRepository.findPersonByEmail(any())).thenReturn(Optional.of(person));
        when(personsRepository.getPersonByCityAndIdNotIn(any(), any(), any())).thenReturn(Page.empty());
        when(personsRepository.getPersonByIdNotInOrderByRegDateDesc(any(), any())).thenReturn(Page.empty());
        when(personCacheService.getPersonByContext()).thenReturn(person);
        friendsService.getFriendsRecommendation();
        verify(personsRepository, times(1)).getPersonByCityAndIdNotIn(any(), any(), any());
        verify(personsRepository, times(1)).getPersonByIdNotInOrderByRegDateDesc(any(),any());
    }


}