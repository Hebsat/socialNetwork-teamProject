package socialnet.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import javax.mail.SendFailedException;

@Slf4j
@Component
@AllArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

    public void sendSimpleMessage(String to, String subject, String text) throws SendFailedException {
        try {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("testbd@internet.ru");
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);}
        catch (Exception e){
            log.warn("E-mail box does not exist", to, e);
        }
    }
}
