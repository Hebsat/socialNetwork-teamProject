package socialnet.service;

import lombok.RequiredArgsConstructor;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.cloudinary.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import socialnet.api.response.CommonRs;
import socialnet.api.response.NotificationRs;
import socialnet.api.websocket.MessageWs;
import socialnet.errors.NotFoundException;
import socialnet.errors.PersonNotFoundException;
import socialnet.kafka.NotificationsKafkaProducer;
import socialnet.mappers.NotificationMapper;
import socialnet.model.entities.Message;
import socialnet.model.entities.Notification;
import socialnet.model.entities.Person;
import socialnet.model.entities.interfaces.Notificationed;
import socialnet.model.enums.FriendshipStatusTypes;
import socialnet.repository.FriendshipsRepository;
import socialnet.repository.NotificationsRepository;
import socialnet.repository.PersonsRepository;
import socialnet.service.util.NetworkPageRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationsService {

    private final NotificationsRepository notificationsRepository;
    private final PersonsRepository personsRepository;
    private final FriendshipsRepository friendshipsRepository;
    private final NotificationMapper notificationMapper;
    private final SimpMessagingTemplate template;
    private final NotificationsKafkaProducer notificationsKafkaProducer;

    @Value("${socialNetwork.default.page}")
    private int offset;
    @Value("${socialNetwork.default.noteSize}")
    private int size;
    @Value("${socialNetwork.timezone}")
    private String timezone;

    public CommonRs<List<NotificationRs>> getAllNotificationsByPerson(int offset, int perPage) throws Exception {
        Person person = personsRepository.findPersonByEmail(
                SecurityContextHolder.getContext().getAuthentication().getName()).orElseThrow(new PersonNotFoundException("Person is not available"));
        return getAllNotificationsByPerson(offset, perPage, person);
    }

    private CommonRs<List<NotificationRs>> getAllNotificationsByPerson(int offset, int perPage, Person person) {
        Pageable pageable = NetworkPageRequest.of(offset, perPage);
        Page<Notification> notificationPage = notificationsRepository.findAllByPersonAndIsReadIsFalse(person, pageable);
        return CommonRs.<List<NotificationRs>>builder()
                .total(notificationPage.getTotalElements())
                .offset(offset)
                .itemPerPage(perPage)
                .data(notificationPage.getContent().stream()
                        .map(notificationMapper::toNotificationResponse).collect(Collectors.toList()))
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public CommonRs<List<NotificationRs>> markNotificationStatusAsRead(Long notificationId, boolean readAll) throws Exception {
        Person person = personsRepository.findPersonByEmail(
                SecurityContextHolder.getContext().getAuthentication().getName())
                .orElseThrow(new PersonNotFoundException("Person is not available"));
        if (readAll) {
            notificationsRepository.findAllByPersonAndIsReadIsFalse(person).forEach(notification -> {
                notification.setIsRead(true);
                notificationsKafkaProducer.sendMessage(notification);
            });
        } else {
            Notification notification = notificationsRepository.findById(notificationId)
                    .orElseThrow(new NotFoundException("Notification with id " + notificationId + "was not found"));
            notification.setIsRead(true);
            notificationsKafkaProducer.sendMessage(notification);
        }
        return getAllNotificationsByPerson(offset, size, person);
    }

    public void sendNotificationToWs(Notificationed entity, Person person)  {
        Notification notification = new Notification();
        notification.setPerson(person);
        notification.setEntity(entity);
        notification.setIsRead(false);
        notification.setNotificationType(entity.getNotificationType());
        notification.setSentTime(LocalDateTime.now(ZoneId.of(timezone)));
        template.convertAndSend(String.format("/user/%s/queue/notifications", person.getId()),
                notificationMapper.toNotificationResponse(notification));
    }

    @Scheduled(cron = "${socialNetwork.scheduling.birthdays}", zone = "${socialNetwork.timezone}")
    public void birthdaysNotificator() {
        LocalDateTime currentDate = LocalDateTime.now(ZoneId.of(timezone));
        List<Person> personList = personsRepository.findPeopleByBirthDate(currentDate.getMonthValue(), currentDate.getDayOfMonth());
        personList.forEach(person -> friendshipsRepository.findFriendshipsByDstPerson(person).forEach(friendship -> {
            if (friendship.getFriendshipStatus().equals(FriendshipStatusTypes.FRIEND) &&
                    friendship.getSrcPerson().getPersonSettings().getFriendBirthdayNotification()) {
                sendNotificationToWs(person, friendship.getSrcPerson());
                notificationsKafkaProducer.sendMessage(person, friendship.getSrcPerson());
                sendNotificationToTelegramBot(person, friendship.getSrcPerson());
            }
        }));
    }

    public void deleteNotification(Notificationed entity) {
        notificationsRepository.findNotificationByEntity(entity.getNotificationType(), entity)
                .ifPresent(notificationsRepository::delete);
    }

    public void sendNotificationToTelegramBot(Notificationed notificationed, Person person) {
        if (person.getTelegramId() != null) {
            HttpClient httpClient = HttpClientBuilder.create().build();
            try {
                HttpPost request = new HttpPost("http://194.87.244.66:8087/bot?userId=" + person.getTelegramId());
                JSONObject jsonObject = new JSONObject()
                        .put("type", notificationed.getNotificationType())
                        .put("info", notificationed.getSimpleInfo())
                        .put("author", new JSONObject()
                                .put("first_name", notificationed.getAuthor().getFirstName())
                                .put("last_name", notificationed.getAuthor().getLastName()));
                StringEntity params = new StringEntity(jsonObject.toString(), StandardCharsets.UTF_8);
                request.addHeader("content-type", "application/json");
                request.setEntity(params);
                httpClient.execute(request);
            } catch (IOException ignore) {
            }
        }
    }

    public void handleMessageForNotification(MessageWs messageWs) {
        Message message = new Message();
        message.setMessageText(messageWs.getMessageText());
        message.setAuthor(personsRepository.findById(messageWs.getAuthorId()).orElse(null));
        Person recipient = personsRepository.findById(messageWs.getRecipientId()).orElseThrow();
        message.setRecipient(recipient);
        if (recipient.getPersonSettings().getMessageNotification() &&
                LocalDateTime.now(ZoneId.of(timezone)).minus(1, ChronoUnit.MINUTES)
                        .isAfter(recipient.getLastOnlineTime())) {
            sendNotificationToWs(message, message.getRecipient());
            sendNotificationToTelegramBot(message, message.getRecipient());
        }
    }
}
