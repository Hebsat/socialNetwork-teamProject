package socialnet.service;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import socialnet.api.request.DialogUserShortListDto;
import socialnet.model.entities.Dialog;
import socialnet.model.entities.Message;
import socialnet.model.entities.Person;
import socialnet.model.enums.ReadStatusTypes;
import socialnet.repository.DialogsRepository;
import socialnet.repository.MessagesRepository;
import socialnet.repository.PersonsRepository;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureEmbeddedDatabase
class DialogsServiceTest {

    @Autowired
    private DialogsService dialogsService;
    @MockBean
    private DialogsRepository dialogsRepository;
    @MockBean
    private MessagesRepository messagesRepository;
    @MockBean
    private PersonsRepository personsRepository;
    @MockBean
    private PersonCacheService personCacheService;


    private Person person1, person2, person3;
    private Dialog dialog1, dialog2;
    private List<Message> messages = new ArrayList<>();
    private List<Dialog> dialogs = new ArrayList<>();


    @BeforeEach
    void setUp() {
        person1 = Person.builder()
                .id(1L)
                .firstName("John")
                .lastName("Trevor")
                .build();
        person2 = Person.builder()
                .id(2L)
                .firstName("Kate")
                .lastName("Bush")
                .build();
        person3 = new Person(3L);
        dialog1 = Dialog.builder()
                .id(1L)
                .firstPerson(person1)
                .secondPerson(person2)
                .lastActiveTime(ZonedDateTime.now())
                .build();
        dialog2 = Dialog.builder()
                .id(2L)
                .firstPerson(person2)
                .secondPerson(person3)
                .lastActiveTime(ZonedDateTime.now())
                .build();
        for (long i = 1; i <= 10; i++) {
            Message message = Message.builder()
                    .id(i)
                    .author(i % 2 != 0 ? person1 : person2)
                    .recipient(i % 2 != 0 ? person2 : person1)
                    .dialog(dialog1)
                    .messageText(UUID.randomUUID().toString())
                    .readStatus(ReadStatusTypes.SENT)
                    .time(ZonedDateTime.now())
                    .build();
            messages.add(message);
        }
        dialog1.setLastMessage(messages.get(9));
        dialogs = List.of(dialog1, dialog2);
        when(personCacheService.getPersonByContext()).thenReturn(person1);
    }

    @AfterEach
    void tearDown() {
        person1 = person2 = person3 = null;
        messages = null;
        dialog1 = dialog2 = null;
    }

    @Test
    void getUnreadMessages() {
        when(messagesRepository.findAllByRecipientAndReadStatusAndIsDeletedFalse(any(), any()))
                .thenReturn(messages.stream().filter(m -> m.getRecipient().equals(person1) && m.getReadStatus().equals(ReadStatusTypes.SENT)).collect(Collectors.toList()));
        assertEquals(5, dialogsService.getUnreadMessages().getData().getCount());
    }

    @Test
    void setReadMessages() {
        when(messagesRepository.findAllByDialogIdAndRecipientAndReadStatusAndIsDeletedFalse(any(), any(), any()))
                .thenReturn(messages.stream().filter(m -> m.getRecipient().equals(person1) && m.getReadStatus().equals(ReadStatusTypes.SENT)).collect(Collectors.toList()));
        assertEquals(5, dialogsService.setReadMessages(1L).getData().getCount());
    }

    @Test
    void beginDialog() throws Exception {
        when(personsRepository.findPersonById(any())).thenReturn(Optional.of(person2));
        when(dialogsRepository.findDialogByFirstPersonAndSecondPerson(person2, person1)).thenReturn(Optional.empty());
        when(dialogsRepository.findDialogByFirstPersonAndSecondPerson(person1, person2)).thenReturn(Optional.of(dialog1));
        when(dialogsRepository.countAllByFirstPersonAndIsDeletedFalseOrSecondPersonAndIsDeletedFalse(person1, person1)).thenReturn(1L);
        DialogUserShortListDto userShortListDto = new DialogUserShortListDto();
        userShortListDto.setUserIds(List.of(2L));
        assertEquals(1, dialogsService.beginDialog(userShortListDto).getData().getCount());
    }

    @Test
    void getAllDialogs() throws Exception {
        when(dialogsRepository.findAllByFirstPersonAndIsDeletedFalseOrSecondPersonAndIsDeletedFalse(person1, person1))
                .thenReturn(dialogs.stream().filter(d -> d.getFirstPerson().equals(person1) || d.getSecondPerson().equals(person1)).collect(Collectors.toList()));
        assertEquals(1, dialogsService.getAllDialogs().getTotal());
    }

    @Test
    void getMessages() {
        when(messagesRepository.findAllByDialogIdAndIsDeletedFalseOrderByTimeAsc(any()))
                .thenReturn(messages.stream().filter(m -> m.getDialog().getId().equals(1L)).collect(Collectors.toList()));
        assertEquals(10, dialogsService.getMessages(1L).getTotal());
    }
}