package socialnet.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import socialnet.api.websocket.MessageCommonWs;
import socialnet.api.websocket.MessageTypingWs;
import socialnet.api.websocket.MessageWs;
import socialnet.errors.NotFoundException;
import socialnet.kafka.MessagesKafkaProducer;
import socialnet.model.entities.Dialog;
import socialnet.model.entities.Message;
import socialnet.repository.DialogsRepository;
import socialnet.repository.MessagesRepository;

import java.util.Comparator;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageWsService {

    private final SimpMessagingTemplate messagingTemplate;
    private final DialogsService dialogsService;
    private final NotificationsService notificationsService;
    private final MessagesKafkaProducer messagesKafkaProducer;
    private final MessagesRepository messagesRepository;
    private final DialogsRepository dialogsRepository;

    public void postMessage(MessageWs messageWs) throws Exception {
        Long id = messageWs.getDialogId() + messageWs.getAuthorId() + System.currentTimeMillis();
        Long recipientId = dialogsService.getRecipientFromDialog(messageWs.getAuthorId(), messageWs.getDialogId()).getId();
        messageWs.setId(id);
        messageWs.setRecipientId(recipientId);
        messagingTemplate.convertAndSendToUser(messageWs.getDialogId().toString(), "/queue/messages", messageWs);
        notificationsService.handleMessageForNotification(messageWs);
        messagesKafkaProducer.sendMessage(messageWs);
    }

    public void messageTypingFromWs(Long dialogId, MessageTypingWs messageTypingWs) {
        messagingTemplate.convertAndSendToUser(dialogId.toString(), "/queue/messages", messageTypingWs);
    }

    public void changeMessage(MessageCommonWs messageCommonWs) throws Exception {
        Message message = getMessage(messageCommonWs);
        messagingTemplate.convertAndSendToUser(message.getDialog().getId().toString(), "/queue/messages", messageCommonWs);
        message.setMessageText(messageCommonWs.getMessageText());
        messagesRepository.save(message);
    }

    public void removeMessage(MessageCommonWs messages) throws Exception {
        Dialog dialog = getDialog(messages);
        messagingTemplate.convertAndSendToUser(messages.getDialogId().toString(), "/queue/messages", messages);
        messages.getMessageIds()
                .forEach(id -> {
                    Message message = messagesRepository.findById(id).orElse(null);
                    if (message != null) {
                        if (message.getId().equals(dialog.getLastMessage().getId())) {
                            dialog.setLastMessage(null);
                            dialogsRepository.save(dialog);
                        }
                        message.setIsDeleted(true);
                        messagesRepository.save(message);
                        if (dialog.getLastMessage() == null) {
                            dialog.setLastMessage(getLastMessage(messages.getDialogId()));
                            dialogsRepository.save(dialog);
                        }
                    }
                });
    }

    public void restoreMessage(MessageCommonWs messageCommonWs) throws Exception {
        Message message = getMessage(messageCommonWs);
        Dialog dialog = getDialog(messageCommonWs);
        messagingTemplate.convertAndSendToUser(messageCommonWs.getDialogId().toString(), "/queue/messages", messageCommonWs);
        message.setIsDeleted(false);
        messagesRepository.save(message);
        if (dialog.getLastMessage() == null || message.getTime().isAfter(dialog.getLastMessage().getTime())) {
            dialog.setLastMessage(message);
            dialogsRepository.save(dialog);
        }
    }

    public void closeDialog(MessageCommonWs messageCommonWs) throws Exception {
        Dialog dialog = getDialog(messageCommonWs);
        if (dialog.getLastMessage() == null) {
            dialog.setIsDeleted(true);
            dialogsRepository.save(dialog);
        }
    }

    private Message getMessage(MessageCommonWs messageCommonWs) throws Exception {
        return messagesRepository.findById(messageCommonWs.getMessageId()).orElseThrow(new NotFoundException("Message not found!"));
    }


    private Message getLastMessage(Long dialogId) {
        return messagesRepository.findAllByDialogIdAndIsDeletedFalse(dialogId)
                .stream()
                .max(Comparator.comparing(Message::getTime)).orElse(null);
    }

    private Dialog getDialog(MessageCommonWs messageCommonWs) throws Exception {
        return dialogsRepository.findById(messageCommonWs.getDialogId()).orElseThrow(new NotFoundException("Dialog not found!"));
    }
}