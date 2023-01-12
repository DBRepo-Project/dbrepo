package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.config.ReadyConfig;
import at.tuwien.exception.SecretInvalidException;
import at.tuwien.repositories.TimeSecretRepository;
import at.tuwien.repositories.UserRepository;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@Log4j2
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class TimeSecretUnitTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @Autowired
    private TimeSecretService timeSecretService;

    @Autowired
    private TimeSecretRepository timeSecretRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    public void beforeEach() {
        userRepository.save(USER_1);
        timeSecretRepository.save(TIME_SECRET_1);
    }

    @Test
    public void updateVerification_succeeds() throws SecretInvalidException {

        /* test */
        timeSecretService.invalidate(TIME_SECRET_1_TOKEN);
        assertThrows(SecretInvalidException.class, () -> {
            timeSecretService.invalidate(TIME_SECRET_1_TOKEN);
        });
    }

}
