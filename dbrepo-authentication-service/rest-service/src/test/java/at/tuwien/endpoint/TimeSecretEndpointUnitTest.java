package at.tuwien.endpoint;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.user.UserForgotDto;
import at.tuwien.endpoints.TimeSecretEndpoint;
import at.tuwien.entities.user.TimeSecret;
import at.tuwien.entities.user.Token;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.repositories.TimeSecretRepository;
import at.tuwien.repositories.TokenRepository;
import at.tuwien.repositories.UserRepository;
import at.tuwien.service.MailService;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.thymeleaf.context.Context;

import javax.servlet.http.HttpServletResponse;
import java.nio.file.AccessDeniedException;
import java.security.Principal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class TimeSecretEndpointUnitTest extends BaseUnitTest {

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private TimeSecretRepository timeSecretRepository;

    @MockBean
    private TokenRepository tokenRepository;

    @MockBean
    private MailService mailService;

    @Autowired
    private TimeSecretEndpoint timeSecretEndpoint;

    @Test
    public void verifyEmail_anonymous_succeeds() throws SecretInvalidException, NotAllowedException {

        /* test */
        verifyEmail_generic(TIME_SECRET_1, USER_1, null, TOKEN_1_TOKEN);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void verifyEmail_researcher_succeeds() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            verifyEmail_generic(TIME_SECRET_1, USER_1, USER_1_PRINCIPAL, TOKEN_1_TOKEN);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"DEVELOPER"})
    public void verifyEmail_developer_succeeds() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            verifyEmail_generic(TIME_SECRET_1, USER_1, USER_1_PRINCIPAL, TOKEN_1_TOKEN);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"DATA_STEWARD"})
    public void verifyEmail_dataSteward_succeeds() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            verifyEmail_generic(TIME_SECRET_1, USER_1, USER_1_PRINCIPAL, TOKEN_1_TOKEN);
        });
    }

    @Test
    public void resend_anonymous_succeeds() throws UserNotFoundException, UserEmailAlreadyVerifiedException,
            UserEmailFailedException, NotAllowedException {
        final UserForgotDto request = UserForgotDto.builder()
                .username(USER_1_USERNAME)
                .email(USER_1_EMAIL)
                .build();
        final User user = User.builder()
                .id(USER_1_ID)
                .username(USER_1_USERNAME)
                .email(USER_1_EMAIL)
                .emailVerified(false)
                .build();

        /* test */
        resend_generic(user, null, request);
    }

    @Test
    public void resend_anonymousAlreadyVerified_succeeds() {
        final UserForgotDto request = UserForgotDto.builder()
                .username(USER_1_USERNAME)
                .email(USER_1_EMAIL)
                .build();

        /* test */
        assertThrows(UserEmailAlreadyVerifiedException.class, () -> {
            resend_generic(USER_1, null, request);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void resend_researcher_succeeds() {
        final UserForgotDto request = UserForgotDto.builder()
                .username(USER_1_USERNAME)
                .email(USER_1_EMAIL)
                .build();

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            resend_generic(USER_1, USER_1_PRINCIPAL, request);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void resend_researcherDifferent_succeeds() {
        final UserForgotDto request = UserForgotDto.builder()
                .username(USER_2_USERNAME)
                .email(USER_2_EMAIL)
                .build();

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            resend_generic(USER_2, USER_2_PRINCIPAL, request);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void resend_developer_succeeds() {
        final UserForgotDto request = UserForgotDto.builder()
                .username(USER_2_USERNAME)
                .email(USER_2_EMAIL)
                .build();

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            resend_generic(USER_2, USER_2_PRINCIPAL, request);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void resend_dataSteward_succeeds() {
        final UserForgotDto request = UserForgotDto.builder()
                .username(USER_3_USERNAME)
                .email(USER_3_EMAIL)
                .build();

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            resend_generic(USER_3, USER_3_PRINCIPAL, request);
        });
    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    protected void verifyEmail_generic(TimeSecret timeSecret, User user, Principal principal, String token)
            throws SecretInvalidException, NotAllowedException {
        final HttpServletResponse mock = new MockHttpServletResponse();

        /* mock */
        when(timeSecretRepository.findByToken(anyString()))
                .thenReturn(Optional.of(timeSecret));
        when(timeSecretRepository.save(any(TimeSecret.class)))
                .thenReturn(TimeSecret.builder()
                        .id(timeSecret.getId())
                        .uid(timeSecret.getUid())
                        .token(timeSecret.getToken())
                        .processed(true)
                        .validTo(timeSecret.getValidTo())
                        .build());
        when(userRepository.save(any(User.class)))
                .thenReturn(user);

        /* test */
        timeSecretEndpoint.verifyEmail(token, mock, principal);
        final String authentication = mock.getHeader("Location");
        assertNotNull(authentication);
        assertTrue(authentication.contains("/login"));
        assertEquals(302, mock.getStatus());
    }

    protected void resend_generic(User user, Principal principal, UserForgotDto data) throws UserEmailFailedException,
            UserNotFoundException, UserEmailAlreadyVerifiedException, NotAllowedException {

        /* mock */
        if (user != null) {
            when(userRepository.findByUsernameOrEmail(data.getUsername(), data.getEmail()))
                    .thenReturn(Optional.of(user));
        } else {
            when(userRepository.findByUsernameOrEmail(data.getUsername(), data.getEmail()))
                    .thenReturn(Optional.empty());
        }
        when(timeSecretRepository.save(any(TimeSecret.class)))
                .thenReturn(TIME_SECRET_1);
        when(userRepository.save(any(User.class)))
                .thenReturn(user);
        doNothing()
                .when(mailService)
                .send(eq(user), anyString(), anyString(), any(Context.class));

        /* test */
        final ResponseEntity<?> response = timeSecretEndpoint.resend(data, principal);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNull(response.getBody());
    }

}
