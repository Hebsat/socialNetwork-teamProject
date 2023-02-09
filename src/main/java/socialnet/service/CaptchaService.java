package socialnet.service;

import com.github.cage.Cage;
import com.github.cage.GCage;
import lombok.AllArgsConstructor;
import socialnet.api.response.CaptchaRs;
import socialnet.model.entities.Captcha;
import socialnet.repository.CaptchaRepository;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
@AllArgsConstructor
@EnableScheduling
public class CaptchaService {
    public static final long HOUR_IN_MILLISECONDS = 3_600_000;
    private CaptchaRepository captchaRepository;

    public CaptchaRs getCaptchaCode() {
        CaptchaRs captchaResponse = new CaptchaRs();
        Cage cage = new GCage();
        String secret = UUID.randomUUID().toString();
        String code = cage.getTokenGenerator().next().substring(0, 4);
        String image = "data:image/png;base64, " + Base64.getEncoder().encodeToString(cage.draw(code));
        Captcha captcha = new Captcha();
        captcha.setCode(code);
        captcha.setSecretCode(secret);
        captcha.setTime(LocalDateTime.now());
        captchaRepository.save(captcha);
        captchaResponse.setCode(secret);
        captchaResponse.setImage(image);
        return captchaResponse;
    }

    @Scheduled(fixedRate = HOUR_IN_MILLISECONDS)
    public void deleteOldCaptchas() {
        captchaRepository.deleteAll(captchaRepository.findOldCaptchas());
    }
}
