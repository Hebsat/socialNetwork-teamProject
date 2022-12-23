package soialNetworkApp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import soialNetworkApp.api.request.MessageWsRq;
import soialNetworkApp.mappers.DialogMapper;
import soialNetworkApp.model.entities.Message;
import soialNetworkApp.repository.DialogsRepository;
import soialNetworkApp.repository.MessagesRepository;
import soialNetworkApp.repository.PersonsRepository;

@Service
@RequiredArgsConstructor
public class MessageWsService {

    private final SimpMessagingTemplate messagingTemplate;
    private final DialogMapper dialogMapper;
    private final DialogsRepository dialogsRepository;
    private final PersonsRepository personsRepository;
    private final MessagesRepository messagesRepository;

    public void getMessageFromWs(MessageWsRq messageWsRq) {
        messagingTemplate.convertAndSendToUser(messageWsRq.getDialogId().toString(), "/queue/messages", messageWsRq);
        Message message = dialogMapper.toMessageFromWs(messageWsRq,
                dialogsRepository.findById(messageWsRq.getDialogId()).orElseThrow(),
                personsRepository.findPersonById(messageWsRq.getAuthorId()).orElseThrow());
        messagesRepository.save(message);
    }

    public void messageTypingFromWs(Long dialogId, Long userId, MessageWsRq messageWsRq) {
        messagingTemplate.convertAndSendToUser(dialogId.toString(), "/queue/messages",
                dialogMapper.toMessageTypingWsRs(userId, dialogId, messageWsRq.getTyping()));
    }
}