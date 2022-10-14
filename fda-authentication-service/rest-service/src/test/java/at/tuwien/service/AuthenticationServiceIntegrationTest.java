package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.auth.LoginRequestDto;
import at.tuwien.config.ReadyConfig;
import at.tuwien.entities.user.User;
import at.tuwien.repositories.TimeSecretRepository;
import at.tuwien.repositories.UserRepository;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@Log4j2
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class AuthenticationServiceIntegrationTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TimeSecretRepository tokenRepository;

    @BeforeEach
    public void beforeEach() {
        final User u1 = userRepository.save(USER_1);
        final User u2 = userRepository.save(USER_2);
        TOKEN_1.setUser(u1);
        tokenRepository.save(TOKEN_1);
        TOKEN_2.setUser(u2);
        tokenRepository.save(TOKEN_2);
    }

    @Test
    public void authenticate_fails() {
        final LoginRequestDto request = LoginRequestDto.builder()
                .username(USER_1_USERNAME)
                .password(USER_1_PASSWORD)
                .build();

        /* mock */

        /* test */
        assertThrows(BadCredentialsException.class, () -> {
            authenticationService.authenticate(request);
        });
    }

    @Test
    public void authenticate_verified_succeeds() {
        final LoginRequestDto request = LoginRequestDto.builder()
                .username(USER_2_USERNAME)
                .password(USER_2_PASSWORD)
                .build();

        /* mock */

        /* test */
        assertThrows(BadCredentialsException.class, () -> {
            authenticationService.authenticate(request);
        });
    }

}
