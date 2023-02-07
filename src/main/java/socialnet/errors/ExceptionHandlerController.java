package socialnet.errors;

import lombok.RequiredArgsConstructor;
import socialnet.api.response.ErrorRs;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@RequiredArgsConstructor
public class ExceptionHandlerController {

    //400
    @ExceptionHandler({
            NotFoundException.class,
            EmptyFieldException.class,
            BadCredentialsException.class,
            PasswordException.class,
            CaptchaException.class,
            FileException.class,
            WrongEmailException.class,
            PersonException.class
    })
    public ResponseEntity<ErrorRs> handleBadRequest(Exception e) {
        return buildResponseEntity(400, e);
    }

    //401
    @ExceptionHandler({
            PersonNotFoundException.class,
            BadAuthorizationException.class
    })
    public ResponseEntity<ErrorRs> handleUnauthorized(Exception e) {
        return buildResponseEntity(401, e);
    }

    //204
    @ExceptionHandler(IncorrectRequestTypeException.class)
    public ResponseEntity<ErrorRs> handleIncorrectRequestTypeException(Exception e) {
        return buildResponseEntity(204, e);
    }

    //403
    @ExceptionHandler(UserPageBlockedException.class)
    public ResponseEntity<ErrorRs> handleForbidden(Exception e) {
        return buildResponseEntity(403, e);
    }

    private ResponseEntity<ErrorRs> buildResponseEntity(int status, Exception e) {
        return ResponseEntity.status(status).body(ErrorRs.builder()
                .error(e.getClass().getSimpleName())
                .timestamp(System.currentTimeMillis())
                .errorDescription(e.getMessage())
                .build());
    }
}
