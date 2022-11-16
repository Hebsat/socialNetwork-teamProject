package main.api.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.*;
import javax.validation.executable.ValidateOnExecution;

@Getter
@Setter
public class RegisterRq {
    private static final int PASSWORD_LENGTH = 6;
    @Email
    private String email;
    @NotBlank(message = "Введите корректное имя")
    @Pattern(regexp = "[А-Яа-яA-Za-z]",
            message = "Введите корректное имя")
    private String firstName;
    @Pattern(regexp = "[А-Яа-яA-Za-z]",
            message = "Введите корректное имя")
    private String lastName;
    private String code;
    @JsonProperty("secret_code")
    private String codeSecret;
    @Min(PASSWORD_LENGTH)
    private String passwd1;
    private String passwd2;
}