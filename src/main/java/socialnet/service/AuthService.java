package socialnet.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import socialnet.api.request.LoginRq;
import socialnet.api.response.CommonRs;
import socialnet.api.response.ComplexRs;
import socialnet.api.response.PersonRs;
import socialnet.errors.PasswordException;
import socialnet.errors.WrongEmailException;
import socialnet.mappers.PersonMapper;
import socialnet.model.entities.Person;
import socialnet.security.jwt.JWTUtil;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final PersonCacheService personCacheService;
    private final PasswordEncoder passwordEncoder;
    private final PersonMapper personMapper;
    private final JWTUtil jwtUtil;

    public CommonRs<PersonRs> loginUser(LoginRq loginRq) throws PasswordException, WrongEmailException {
        Person person = personCacheService.getPersonByEmail(loginRq.getEmail());
        if (person != null) {
            if (!passwordEncoder.matches(loginRq.getPassword(), person.getPassword())) {
                throw new PasswordException("Wrong password for email: '" + loginRq.getEmail() + "'");
            } else {
                return buildCommonResponse(person);
            }
        } else {
            throw new WrongEmailException("User with email: '" + loginRq.getEmail() + "' not found");
        }
    }

    public CommonRs<ComplexRs> logoutUser() {
        SecurityContextHolder.clearContext();
        return CommonRs.<ComplexRs>builder()
                .timestamp(System.currentTimeMillis())
                .data(ComplexRs.builder().build())
                .build();
    }

    private CommonRs<PersonRs> buildCommonResponse(Person person) {
        PersonRs user = personMapper.toPersonResponse(person);
        user.setToken(jwtUtil.createToken(person.getEmail()));
        return CommonRs.<PersonRs>builder()
                .timestamp(System.currentTimeMillis())
                .data(user)
                .build();
    }
}
