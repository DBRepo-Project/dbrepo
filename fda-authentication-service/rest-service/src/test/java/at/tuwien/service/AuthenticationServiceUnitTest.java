package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.auth.LoginRequestDto;
import at.tuwien.auth.MariaDbPassword;
import at.tuwien.config.ReadyConfig;
import at.tuwien.entities.user.User;
import at.tuwien.repositories.TimeSecretRepository;
import at.tuwien.repositories.UserRepository;
import com.rabbitmq.client.Channel;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class AuthenticationServiceUnitTest extends BaseUnitTest {

    @MockBean
    private Channel channel;

    @Test
    public void authenticate_verified_succeeds() {
        final String plaintext = "dbrepo";
        final String cipher = "*2F3AA43960B265DA32530022AE62B16E97BE51C3";

        /* mock */

        /* test */
        final String response = MariaDbPassword.encode(plaintext);
        assertEquals(cipher, response);
    }

}
