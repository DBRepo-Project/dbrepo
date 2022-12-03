package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.auth.JwtUtils;
import at.tuwien.config.ReadyConfig;
import at.tuwien.entities.user.Token;
import at.tuwien.exception.TokenNotEligableException;
import at.tuwien.exception.TokenNotFoundException;
import at.tuwien.exception.UserNotFoundException;
import at.tuwien.repositories.TokenRepository;
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

import javax.servlet.ServletException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertThrows;

@Log4j2
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class TokenIntegrationTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TokenRepository tokenRepository;

    @Autowired
    private JwtUtils jwtUtils;

    @BeforeEach
    public void beforeEach() {
        userRepository.save(USER_1);
    }

    @Test
    public void check_succeeds() throws ServletException {

        /* mock */
        final String jwt = jwtUtils.generateJwtToken(USER_1_USERNAME, TOKEN_1_EXPIRES);
        final Token entity = Token.builder()
                .token(jwt)
                .tokenHash(JwtUtils.toHash(jwt))
                .creator(USER_1_ID)
                .expires(TOKEN_1_EXPIRES)
                .build();
        final Token token = tokenRepository.save(entity);

        /* test */
        tokenService.check(jwt);
    }

    @Test
    public void check_revoked_fails() {

        /* mock */
        final String jwt = jwtUtils.generateJwtToken(USER_1_USERNAME, TOKEN_1_EXPIRES);
        final Token entity = Token.builder()
                .token(jwt)
                .tokenHash(JwtUtils.toHash(jwt))
                .creator(USER_1_ID)
                .expires(TOKEN_1_EXPIRES)
                .deleted(Instant.now().minus(1, ChronoUnit.SECONDS))
                .build();
        final Token token = tokenRepository.save(entity);

        /* test */
        assertThrows(ServletException.class, () -> {
            tokenService.check(jwt);
        });
    }

    @Test
    public void create_userNotFound_fails() {

        /* test */
        assertThrows(UserNotFoundException.class, () -> {
            tokenService.create(USER_2_PRINCIPAL);
        });
    }

}
