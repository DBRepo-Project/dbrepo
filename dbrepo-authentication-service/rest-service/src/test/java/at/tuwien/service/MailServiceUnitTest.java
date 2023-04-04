package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.config.ReadyConfig;
import at.tuwien.exception.UserEmailFailedException;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.thymeleaf.context.Context;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@Log4j2
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@TestPropertySource(properties = {"spring.mail.username=test"})
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class MailServiceUnitTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @Autowired
    private MailService mailService;

    @MockBean
    private JavaMailSender mailSender;

    @Test
    public void send_succeeds() throws UserEmailFailedException {
        final Context context = new Context();
        context.setVariable("username", USER_1_USERNAME);

        /* test */
        mailService.send(USER_1, "Test", "mail-welcome.txt", context);
    }

    @Test
    public void send_fails() {
        final Context context = new Context();
        context.setVariable("username", USER_1_USERNAME);

        /* mock */
        doThrow(MailSendException.class).when(mailSender)
                .send(any(SimpleMailMessage.class));

        /* test */
        assertThrows(UserEmailFailedException.class, () -> {
            mailService.send(USER_1, "Test", "mail-welcome.txt", context);
        });
    }

}
