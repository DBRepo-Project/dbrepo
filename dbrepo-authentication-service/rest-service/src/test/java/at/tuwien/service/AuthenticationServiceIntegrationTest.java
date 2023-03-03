package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.auth.LoginRequestDto;
import at.tuwien.config.ReadyConfig;
import at.tuwien.repositories.TimeSecretRepository;
import at.tuwien.repositories.UserRepository;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class AuthenticationServiceIntegrationTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private TimeSecretRepository timeSecretRepository;

    @Autowired
    private AuthenticationService authenticationService;

    @BeforeEach
    public void beforeEach() {
        TIME_SECRET_1.setUser(USER_1);
        TIME_SECRET_2.setUser(USER_2);
    }

    @Test
    @Disabled
    public void authenticate_fails() {
        final LoginRequestDto request = LoginRequestDto.builder()
                .username(USER_1_USERNAME)
                .password(USER_1_PASSWORD)
                .build();

        /* mock */
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));
        when(timeSecretRepository.findByToken(TIME_SECRET_1_TOKEN))
                .thenReturn(Optional.of(TIME_SECRET_1));

        /* test */
        assertThrows(BadCredentialsException.class, () -> {
            authenticationService.authenticate(request);
        });
    }

    @Test
    @Disabled
    public void authenticate_verified_succeeds() {
        final LoginRequestDto request = LoginRequestDto.builder()
                .username(USER_2_USERNAME)
                .password(USER_2_PASSWORD)
                .build();

        /* mock */
        when(userRepository.findByUsername(USER_2_USERNAME))
                .thenReturn(Optional.of(USER_2));
        when(timeSecretRepository.findByToken(TIME_SECRET_2_TOKEN))
                .thenReturn(Optional.of(TIME_SECRET_2));

        /* test */
        assertThrows(BadCredentialsException.class, () -> {
            authenticationService.authenticate(request);
        });
    }

}
