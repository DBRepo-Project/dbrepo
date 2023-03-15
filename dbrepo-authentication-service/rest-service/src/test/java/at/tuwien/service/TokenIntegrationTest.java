package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.auth.JwtUtils;
import at.tuwien.config.H2Utils;
import at.tuwien.config.ReadyConfig;
import at.tuwien.entities.user.Token;
import at.tuwien.exception.TokenNotEligableException;
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

    @Autowired
    private H2Utils h2Utils;

    @BeforeEach
    public void beforeEach() {
        userRepository.save(USER_1);
        h2Utils.runScript("view.sql");
    }

    @Test
    public void check_succeeds() throws ServletException, UserNotFoundException, TokenNotEligableException {

        /* mock */
        tokenService.create(USER_1_PRINCIPAL);
        final Token token = tokenService.create(USER_1_PRINCIPAL);

        /* test */
        tokenService.check(token.getToken());
    }

    @Test
    public void check_revoked_fails() throws UserNotFoundException, TokenNotEligableException {

        /* mock */
        final Token token = tokenService.create(USER_1_PRINCIPAL);

        /* test */
        assertThrows(ServletException.class, () -> {
            tokenService.check(token.getToken());
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
