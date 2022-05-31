package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.config.ReadyConfig;
import at.tuwien.entities.user.User;
import at.tuwien.exception.UserEmailFailedException;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.thymeleaf.context.Context;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;

@Log4j2
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class MailServiceIntegrationTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @Autowired
    private MailService mailService;

    @Test
    public void send_succeeds() throws UserEmailFailedException {
        final User user = User.builder()
                .username(USER_1_USERNAME)
                .password(USER_1_PASSWORD)
                .firstname(USER_1_FIRSTNAME)
                .lastname(USER_1_LASTNAME)
                .email("martinweiseat@gmail.com")
                .build();
        final Context context = new Context();
        context.setVariable("username", user.getUsername());

        /* test */
        mailService.send(user, "Test", "welcome-mail.txt", context);
    }

    @Test
    public void send_fails() {
        final User user = User.builder()
                .username(USER_1_USERNAME)
                .password(USER_1_PASSWORD)
                .firstname(USER_1_FIRSTNAME)
                .lastname(USER_1_LASTNAME)
                .email("doesnotexist@gmail.com")
                .build();
        final Context context = new Context();
        context.setVariable("username", user.getUsername());

        /* test */
        assertThrows(UserEmailFailedException.class, () -> {
            mailService.send(user, "Test", "welcome-mail.txt", context);
        });
    }

}
