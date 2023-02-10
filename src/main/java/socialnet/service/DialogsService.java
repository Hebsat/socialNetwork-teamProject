package socialnet.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import socialnet.api.request.DialogUserShortListDto;
import socialnet.api.response.CommonRs;
import socialnet.api.response.ComplexRs;
import socialnet.api.response.DialogRs;
import socialnet.api.response.MessageRs;
import socialnet.errors.NotFoundException;
import socialnet.mappers.DialogMapper;
import socialnet.model.entities.Dialog;
import socialnet.model.entities.Friendship;
import socialnet.model.entities.Message;
import socialnet.model.entities.Person;
import socialnet.model.enums.FriendshipStatusTypes;
import socialnet.model.enums.ReadStatusTypes;
import socialnet.repository.DialogsRepository;
import socialnet.repository.FriendshipsRepository;
import socialnet.repository.MessagesRepository;
import socialnet.repository.PersonsRepository;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DialogsService {

    private final FriendshipsRepository friendshipsRepository;
    private final DialogsRepository dialogsRepository;
    private final MessagesRepository messagesRepository;
    private final PersonsRepository personsRepository;
    private final DialogMapper dialogMapper;
    private final DialogMapService dialogMapService;
    private final PersonCacheService personCacheService;

    public CommonRs<ComplexRs> getUnreadMessages() {
        return new CommonRs<>(new ComplexRs((long) messagesRepository
                .findAllByRecipientAndReadStatusAndIsDeletedFalse(personCacheService.getPersonByContext(), ReadStatusTypes.SENT)
                .size()));
    }

    public CommonRs<ComplexRs> setReadMessages(Long dialogId) {
        final Long[] readCount = {0L};
        messagesRepository.findAllByDialogIdAndRecipientAndReadStatusAndIsDeletedFalse(dialogId, personCacheService.getPersonByContext(), ReadStatusTypes.SENT)
                .forEach(m -> {
                    m.setReadStatus(ReadStatusTypes.READ);
                    messagesRepository.save(m);
                    readCount[0]++;
                });
        return new CommonRs<>(new ComplexRs(readCount[0]));
    }

    public CommonRs<ComplexRs> beginDialog(DialogUserShortListDto dialogUserShortListDto) throws Exception {
        Person currentPerson = personCacheService.getPersonByContext();
        Person anotherPerson = personsRepository.findPersonById(dialogUserShortListDto.getUserIds().get(0)).orElseThrow(new NotFoundException("Not such Person!"));
        Dialog dialog = dialogsRepository.findDialogByFirstPersonAndSecondPerson(currentPerson, anotherPerson)
                .orElse(dialogsRepository.findDialogByFirstPersonAndSecondPerson(anotherPerson, currentPerson)
                        .orElse(new Dialog(currentPerson, anotherPerson, ZonedDateTime.now(), false)));
        if (dialog.getIsDeleted().equals(true)) {
            dialog.setIsDeleted(false);
        }
        dialogsRepository.save(dialog);
        return new CommonRs<>(new ComplexRs(dialogsRepository.countAllByFirstPersonAndIsDeletedFalseOrSecondPersonAndIsDeletedFalse(currentPerson, currentPerson)));
    }

    public CommonRs<List<DialogRs>> getAllDialogs() throws Exception {
        List<DialogRs> dialogRsList = createDialogRsList(personCacheService.getPersonByContext());
        dialogRsList = blockDialogs(dialogRsList);
        return new CommonRs<>(dialogRsList, (long) dialogRsList.size());
    }

    public CommonRs<List<MessageRs>> getMessages(Long dialogId) {
        List<MessageRs> messagesRs = new ArrayList<>();
        messagesRepository.findAllByDialogIdAndIsDeletedFalseOrderByTimeAsc(dialogId)
                .forEach(m -> messagesRs.add(dialogMapper.toMessageRs(m)));
        return new CommonRs<>(messagesRs, (long) messagesRs.size());
    }

    private Person getRecipientForLastMessage(Message message) {
        return dialogMapService.isAuthor(message) ? message.getRecipient() : message.getAuthor();
    }

    private List<DialogRs> createDialogRsList(Person person) throws Exception {
        List<DialogRs> dialogRsList = new ArrayList<>();
        for (Dialog d : dialogsRepository.findAllByFirstPersonAndIsDeletedFalseOrSecondPersonAndIsDeletedFalse(person, person)) {
            checkLastMessageRs(d);
            Message message = d.getLastMessage();
            message.setRecipient(getRecipientForLastMessage(message));
            dialogRsList.add(dialogMapper.toDialogRs(d, dialogMapper.toMessageRs(message)));
        }
        return dialogRsList;
    }

    private void checkLastMessageRs(Dialog dialog) throws Exception {
        if (dialog.getLastMessage() == null) {
            Person currentPerson = personCacheService.getPersonByContext();
            Person anotherPerson = getRecipientFromDialog(currentPerson.getId(), dialog.getId());
            dialog.setLastMessage(Message.builder()
                    .author(currentPerson)
                    .recipient(anotherPerson)
                    .build());
        }
    }

    public Person getRecipientFromDialog(Long authorId, Long dialogId) throws Exception {
        Dialog dialog = dialogsRepository.findById(dialogId).orElseThrow(new NotFoundException("No such Dialog!"));
        return !authorId.equals(dialog.getFirstPerson().getId()) ?
                dialog.getFirstPerson() :
                dialog.getSecondPerson();
    }

    private List<DialogRs> blockDialogs(List<DialogRs> dialogs) {
        Person me = personCacheService.getPersonByContext();
        List<Friendship> friendships =
                friendshipsRepository.findFriendshipsByFriendshipStatusAndSrcPersonIdOrDstPersonId(FriendshipStatusTypes.BLOCKED, me.getId(), me.getId());
        Set<Long> srcDstPersonsIds = getSrcDstPersonsIds(friendships, me);
        return dialogs
                .stream()
                .filter(dialog -> !srcDstPersonsIds.contains(dialog.getAuthorId()) && !srcDstPersonsIds.contains(dialog.getRecipientId()))
                .collect(Collectors.toList());
    }

    private Set<Long> getSrcDstPersonsIds(List<Friendship> friendships, Person me) {
        Set<Long> ids = new HashSet<>();
        friendships.forEach(friendship -> {
            if (friendship.getDstPerson().getId().equals(me.getId())) {
                ids.add(friendship.getSrcPerson().getId());
            } else {
                ids.add(friendship.getDstPerson().getId());
            }
        });
        return ids;
    }
}