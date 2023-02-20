package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.config.ReadyConfig;
import at.tuwien.entities.user.TimeSecret;
import at.tuwien.entities.user.User;
import at.tuwien.exception.SecretInvalidException;
import at.tuwien.repositories.TimeSecretRepository;
import at.tuwien.repositories.UserRepository;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class TimeSecretUnitTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @MockBean
    private TimeSecretRepository timeSecretRepository;

    @MockBean
    private UserRepository userRepository;

    @Autowired
    private TimeSecretService timeSecretService;

    @Test
    public void updateVerification_succeeds() throws SecretInvalidException {

        /* mock */
        when(timeSecretRepository.findByToken(TIME_SECRET_1_TOKEN))
                .thenReturn(Optional.of(TIME_SECRET_1))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class)))
                .thenReturn(USER_1);
        when(timeSecretRepository.save(any(TimeSecret.class)))
                .thenReturn(TIME_SECRET_1);

        /* test */
        timeSecretService.invalidate(TIME_SECRET_1_TOKEN);
        assertThrows(SecretInvalidException.class, () -> {
            timeSecretService.invalidate(TIME_SECRET_1_TOKEN);
        });
    }

}
