package socialnet.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import socialnet.api.websocket.MessageWs;
import socialnet.api.response.DialogRs;
import socialnet.api.response.MessageRs;
import socialnet.kafka.dto.MessageKafka;
import socialnet.model.entities.Dialog;
import socialnet.model.entities.Message;
import socialnet.model.entities.Person;
import socialnet.model.enums.ReadStatusTypes;
import socialnet.repository.DialogsRepository;
import socialnet.repository.PersonsRepository;
import socialnet.service.DialogMapService;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Mapper(componentModel = "spring", uses = {DialogMapService.class, PersonMapper.class, PersonsRepository.class, DialogsRepository.class})
public interface DialogMapper {

    @Mapping(target = "id", source = "dialog.id")
    @Mapping(target = "lastMessage", source = "messageRs")
    @Mapping(target = "unreadCount", source = "dialog", qualifiedByName = "getUnreadMessagesCountForDialog")
    DialogRs toDialogRs(MessageRs messageRs, Dialog dialog);

    @Mapping(target = "isSentByMe", source="message", qualifiedByName = "isAuthor")
    @Mapping(target = "recipient", source = "person")
    @Mapping(target = "recipientId", source="message.recipient.id")
    @Mapping(target = "authorId", source = "message.author.id")
    @Mapping(target = "readStatus", expression = "java(message.getReadStatus().name())")
    @Mapping(target = "id", source = "message.id")
    MessageRs toMessageRs(Message message, Person person);

    @Mapping(target = "isDeleted", expression = "java(false)")
    MessageKafka toMessageKafkaFromMessageWs(MessageWs messageWs);

    @Mapping(target = "author", source = "authorId")
    @Mapping(target = "recipient", source = "recipientId")
    @Mapping(target = "dialog", source = "dialogId")
    Message toMessageFromKafka(MessageKafka kafka);

    default ZonedDateTime getTime(Long time) {
        return new Timestamp(time).toLocalDateTime().atZone(ZoneId.systemDefault());
    }

    default ReadStatusTypes getReadStatus(String readStatus) {
        return ReadStatusTypes.valueOf(readStatus);
    }
}
