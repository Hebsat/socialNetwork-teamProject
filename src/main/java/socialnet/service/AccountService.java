package socialnet.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import socialnet.api.request.*;
import socialnet.api.response.CommonRs;
import socialnet.api.response.ComplexRs;
import socialnet.api.response.PersonSettingsRs;
import socialnet.api.response.RegisterRs;
import socialnet.errors.CaptchaException;
import socialnet.errors.IncorrectRequestTypeException;
import socialnet.errors.PasswordException;
import socialnet.errors.PersonNotFoundException;
import socialnet.model.entities.Captcha;
import socialnet.model.entities.Person;
import socialnet.model.entities.PersonSettings;
import socialnet.model.enums.MessagePermissionTypes;
import socialnet.model.enums.NotificationTypes;
import socialnet.repository.CaptchaRepository;
import socialnet.repository.PersonSettingsRepository;
import socialnet.repository.PersonsRepository;

import javax.mail.MessagingException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final PersonsRepository personsRepository;
    private final CaptchaRepository captchaRepository;
    private final PersonSettingsRepository personSettingsRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService eMailService;
    private final PersonCacheService personCacheService;
    @Value("${auth.pass-restore}")
    String basePassUrl;
    @Value("${auth.email-restore}")
    String baseEmailUrl;
    @Value("${socialNetwork.timezone}")
    private String timezone;

    public RegisterRs getRegResponse(RegisterRq regRequest) throws PasswordException, CaptchaException {
        RegisterRs registerRs = new RegisterRs();
        ComplexRs data = ComplexRs.builder().message("OK").build();

        if (!regRequest.getPasswd1().equals(regRequest.getPasswd2())) {
            throw new PasswordException("Введены разные пароли");
        }
        String captcha = regRequest.getCode();
        String secret = regRequest.getCodeSecret();
        Optional<Captcha> optionalCaptcha = captchaRepository.findCaptchaBySecretCode(secret);
        if (optionalCaptcha.isPresent()) {
            if (!optionalCaptcha.get().getCode().equals(captcha)) {
                throw new CaptchaException("Введена неверная капча");
            }
        } else {
            throw new CaptchaException("Невалидная капча, обновите страницу или капчу");
        }
        registerRs.setEmail(regRequest.getEmail());
        registerRs.setData(data);
        Person person = new Person();
        person.setFirstName(regRequest.getFirstName());
        person.setLastName(regRequest.getLastName());
        person.setPassword(passwordEncoder.encode(regRequest.getPasswd1()));
        person.setRegDate(LocalDateTime.now(ZoneId.of(timezone)));
        person.setEmail(regRequest.getEmail());
        person.setPhoto("https://res.cloudinary.com/dre3qhjvh/image/upload/v1669013824/default-1_wzqelg.png");
        person.setIsApproved(false);
        person.setIsBlocked(false);
        person.setIsDeleted(false);
        person.setEmail(regRequest.getEmail());
        person.setMessagePermission(MessagePermissionTypes.ALL);
        person.setPersonSettings(createDefaultNotificationsSettings());
        personCacheService.cachePerson(person);
        return registerRs;
    }

    public RegisterRs getPasswordSet(PasswordSetRq passwordSetRq) {
        Person person = personCacheService.getPersonByEmail(SecurityContextHolder
                .getContext().getAuthentication().getName());
        RegisterRs response = new RegisterRs();
        ComplexRs data = getComplexRs();
        response.setEmail(person.getEmail());
        response.setTimestamp(0);
        response.setData(data);

        person.setPassword(passwordEncoder.encode(passwordSetRq.getPassword()));
        personCacheService.cachePerson(person);

        return response;
    }

    public RegisterRs getPasswordReSet(PasswordRq passwordRq) {
        Optional<Person> optPerson = personsRepository.checkToken(passwordRq.getSecret());
        Person rescuePerson = null;
        if (optPerson.isPresent()) {
            rescuePerson = optPerson.get();
        }

        RegisterRs response = new RegisterRs();
        ComplexRs data = getComplexRs();
        response.setEmail(rescuePerson != null ? rescuePerson.getEmail() : null);
        response.setTimestamp(0);
        response.setData(data);

        if (rescuePerson != null) {
            rescuePerson.setPassword(passwordEncoder.encode(passwordRq.getPassword()));
            personCacheService.cachePerson(rescuePerson);
        }

        return response;
    }

    public RegisterRs getPasswordRecovery(String email) throws MessagingException {
        RegisterRs response = new RegisterRs();
        ComplexRs data = getComplexRs();
        response.setEmail(email);
        response.setTimestamp(0);
        response.setData(data);

        Person person = personCacheService.getPersonByEmail(email);
        String token = UUID.randomUUID().toString().replaceAll("-", "");
        person.setChangePasswordToken(token);
        personCacheService.cachePerson(person);

        String subject = "Восстановление пароля";
        String text = basePassUrl + token;
        eMailService.sendSimpleMessage(email, subject, text);

        return response;
    }

    private static ComplexRs getComplexRs() {
        return new ComplexRs(0, 0L, "OK", 0L);
    }

    public RegisterRs getEmailRecovery() throws MessagingException {
        Person person = personCacheService.getPersonByEmail(SecurityContextHolder
                .getContext().getAuthentication().getName());
        RegisterRs response = new RegisterRs();
        ComplexRs data = getComplexRs();
        response.setEmail(person.getEmail());
        response.setTimestamp(0);
        response.setData(data);

        String token = UUID.randomUUID().toString().replaceAll("-", "");
        person.setChangePasswordToken(token);
        personCacheService.cachePerson(person);

        String to = person.getEmail();
        String subject = "Смена ящика почты";
        String text = baseEmailUrl + token;
        eMailService.sendSimpleMessage(to, subject, text);
        return response;
    }
    public RegisterRs getNewEmail(EmailRq emailRq){
        Optional<Person> optPerson = personsRepository.checkToken(emailRq.getSecret());
        Person rescuePerson=null;
        if (optPerson.isPresent()){rescuePerson = optPerson.get();}

        RegisterRs response = new RegisterRs();
        ComplexRs data = getComplexRs();
        response.setEmail(rescuePerson.getEmail());
        response.setTimestamp(0);
        response.setData(data);

        rescuePerson.setEmail(emailRq.getEmail());
        rescuePerson.setChangePasswordToken(null);
        personCacheService.cachePerson(rescuePerson);

        return response;
    }

    private PersonSettings createDefaultNotificationsSettings() {
        PersonSettings settings = new PersonSettings();
        settings.setPostNotification(true);
        settings.setPostCommentNotification(true);
        settings.setCommentCommentNotification(true);
        settings.setLikeNotification(true);
        settings.setFriendBirthdayNotification(true);
        settings.setFriendRequestNotification(true);
        settings.setMessageNotification(true);
        return personSettingsRepository.save(settings);
    }

    public CommonRs<ComplexRs> setPersonSetting(PersonSettingsRq request) throws PersonNotFoundException, IncorrectRequestTypeException {
        Person person = personCacheService.getPersonByContext();
        if (person == null) {
            throw new PersonNotFoundException("Person not found");
        }
        PersonSettings personSettings = person.getPersonSettings();
        switch (request.getNotificationType()) {
            case "POST": {
                personSettings.setPostNotification(request.isEnable());
                break;
            }
            case "POST_COMMENT": {
                personSettings.setPostCommentNotification(request.isEnable());
                break;
            }
            case "COMMENT_COMMENT": {
                personSettings.setCommentCommentNotification(request.isEnable());
                break;
            }
            case "FRIEND_REQUEST": {
                personSettings.setFriendRequestNotification(request.isEnable());
                break;
            }
            case "MESSAGE": {
                personSettings.setMessageNotification(request.isEnable());
                break;
            }
            case "FRIEND_BIRTHDAY": {
                personSettings.setFriendBirthdayNotification(request.isEnable());
                break;
            }
            case "POST_LIKE": {
                personSettings.setLikeNotification(request.isEnable());
                break;
            }
            default:
                throw new IncorrectRequestTypeException("Incorrect notification type");
        }
        personSettingsRepository.save(personSettings);
        return CommonRs.<ComplexRs>builder()
                .timestamp(System.currentTimeMillis())
                .data(new ComplexRs())
                .build();
    }

    public CommonRs<List<PersonSettingsRs>> getPersonSettings() throws PersonNotFoundException {
        Person person = personCacheService.getPersonByContext();
        if (person == null) {
            throw new PersonNotFoundException("Person not found");
        }
        PersonSettings personSettings = person.getPersonSettings();
        PersonSettingsRs postValue = PersonSettingsRs.builder()
                .type(String.valueOf(NotificationTypes.POST)).enable(personSettings.getPostNotification()).build();
        PersonSettingsRs postCommentValue = PersonSettingsRs.builder()
                .type(String.valueOf(NotificationTypes.POST_COMMENT)).enable(personSettings.getPostCommentNotification()).build();
        PersonSettingsRs commentCommentValue = PersonSettingsRs.builder()
                .type(String.valueOf(NotificationTypes.COMMENT_COMMENT)).enable(personSettings.getCommentCommentNotification()).build();
        PersonSettingsRs friendRequestValue = PersonSettingsRs.builder()
                .type(String.valueOf(NotificationTypes.FRIEND_REQUEST)).enable(personSettings.getFriendRequestNotification()).build();
        PersonSettingsRs messageValue = PersonSettingsRs.builder()
                .type(String.valueOf(NotificationTypes.MESSAGE)).enable(personSettings.getMessageNotification()).build();
        PersonSettingsRs friendsBirthdayValue = PersonSettingsRs.builder()
                .type(String.valueOf(NotificationTypes.FRIEND_BIRTHDAY)).enable(personSettings.getFriendBirthdayNotification()).build();
        PersonSettingsRs postLikeValue = PersonSettingsRs.builder()
                .type(String.valueOf(NotificationTypes.POST_LIKE)).enable(personSettings.getLikeNotification()).build();
        return CommonRs.<List<PersonSettingsRs>>builder()
                .timestamp(System.currentTimeMillis())
                .data(List.of(
                        postValue,
                        postCommentValue,
                        commentCommentValue,
                        friendRequestValue,
                        messageValue,
                        friendsBirthdayValue,
                        postLikeValue))
                .build();
    }

    public CommonRs<Boolean> setTelegramProperties(Long userId, Boolean value) {
        Person person = personCacheService.getPersonByContext();
        person.setTelegramId(value ? userId : null);
        personsRepository.save(person);
        return CommonRs.<Boolean>builder().timestamp(System.currentTimeMillis()).data(value).build();
    }
}
