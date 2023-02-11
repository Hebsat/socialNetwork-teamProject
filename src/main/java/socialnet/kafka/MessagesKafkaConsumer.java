package socialnet.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import socialnet.kafka.dto.MessageKafka;
import socialnet.mappers.DialogMapper;
import socialnet.model.entities.Dialog;
import socialnet.model.entities.Message;
import socialnet.repository.DialogsRepository;
import socialnet.repository.MessagesRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessagesKafkaConsumer {

    private final MessagesRepository messagesRepository;
    private final DialogsRepository dialogsRepository;
    private final DialogMapper dialogMapper;
    private final NotificationsKafkaProducer notificationsKafkaProducer;

    @KafkaListener(topics = "messages", autoStartup = "${listen.auto.start:true}")
    public void consume(MessageKafka messageKafka) {
        log.info(String.format("Json received -> %s", messageKafka.toString()));
        Dialog dialog = dialogsRepository.findById(messageKafka.getDialogId()).orElseThrow();
        Message message = dialogMapper.toMessageFromKafka(messageKafka);
        messagesRepository.save(message);
        dialog.setLastMessage(message);
        dialogsRepository.save(dialog);
        notificationsKafkaProducer.sendMessage(message, message.getRecipient());
    }
}
