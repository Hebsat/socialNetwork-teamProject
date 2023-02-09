package socialnet.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import socialnet.api.websocket.MessageWs;
import socialnet.api.response.DialogRs;
import socialnet.api.response.MessageRs;
import socialnet.kafka.dto.MessageKafka;
import socialnet.model.entities.Dialog;
import socialnet.model.entities.Message;
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
    DialogRs toDialogRs(Dialog dialog, MessageRs messageRs);

    @Mapping(target = "isSentByMe", source="message", qualifiedByName = "isAuthor")
    @Mapping(target = "id", source = "message.id")
    @Mapping(target = "authorId", source = "message.author.id")
    @Mapping(target = "recipientId", source = "message.recipient.id")
    @Mapping(target = "recipient", source = "message.recipient")
    MessageRs toMessageRs(Message message);

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

    default String getReadStatus(ReadStatusTypes readStatus) {
        return readStatus == null ? ReadStatusTypes.SENT.name() : readStatus.name();
    }
}
