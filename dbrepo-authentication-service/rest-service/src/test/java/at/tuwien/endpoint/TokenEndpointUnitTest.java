package at.tuwien.endpoint;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.auth.TokenBriefDto;
import at.tuwien.api.auth.TokenDto;
import at.tuwien.api.user.UserForgotDto;
import at.tuwien.endpoints.TimeSecretEndpoint;
import at.tuwien.endpoints.TokenEndpoint;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.thymeleaf.context.Context;

import javax.servlet.http.HttpServletResponse;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class TokenEndpointUnitTest extends BaseUnitTest {

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private TokenRepository tokenRepository;

    @MockBean
    private MailService mailService;

    @Autowired
    private TokenEndpoint tokenEndpoint;

    @Test
    public void listAll_anonymous_fails() {

        /* test */
        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> {
            listAll_generic(USER_1, null, List.of());
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void listAll_notFound_fails() {

        /* test */
        assertThrows(UserNotFoundException.class, () -> {
            listAll_generic(null, USER_1_PRINCIPAL, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void listAll_researcher_succeeds() throws UserNotFoundException {

        /* test */
        listAll_generic(USER_1, USER_1_PRINCIPAL, List.of(TOKEN_1));
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void listAll_developer_succeeds() throws UserNotFoundException {

        /* test */
        listAll_generic(USER_2, USER_2_PRINCIPAL, List.of(TOKEN_2));
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void listAll_dataSteward_succeeds() throws UserNotFoundException {

        /* test */
        listAll_generic(USER_3, USER_3_PRINCIPAL, List.of(TOKEN_3));
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void listAll_researcherEmpty_succeeds() throws UserNotFoundException {

        /* test */
        listAll_generic(USER_1, USER_1_PRINCIPAL, List.of());
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void listAll_developerEmpty_succeeds() throws UserNotFoundException {

        /* test */
        listAll_generic(USER_2, USER_2_PRINCIPAL, List.of());
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void listAll_dataStewardEmpty_succeeds() throws UserNotFoundException {

        /* test */
        listAll_generic(USER_3, USER_3_PRINCIPAL, List.of());
    }

    @Test
    public void create_anonymous_fails() {

        /* test */
        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> {
            create_generic(USER_1, null, TOKEN_1, List.of());
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void create_researcher_succeeds() throws UserNotFoundException, TokenNotEligableException {

        /* test */
        create_generic(USER_1, USER_1_PRINCIPAL, TOKEN_1, List.of());
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void create_researcherLimitExceeded_fails() {

        /* test */
        assertThrows(TokenNotEligableException.class, () -> {
            create_generic(USER_1, USER_1_PRINCIPAL, TOKEN_1, List.of(TOKEN_1));
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void create_developer_succeeds() throws UserNotFoundException, TokenNotEligableException {

        /* test */
        create_generic(USER_2, USER_2_PRINCIPAL, TOKEN_2, List.of());
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void create_developerLimitExceeded_fails() {

        /* test */
        assertThrows(TokenNotEligableException.class, () -> {
            create_generic(USER_2, USER_2_PRINCIPAL, TOKEN_2, List.of(TOKEN_2));
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void create_dataSteward_fails() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            create_generic(USER_3, USER_3_PRINCIPAL, TOKEN_3, List.of());
        });
    }

    @Test
    public void delete_anonymous_fails() {

        /* test */
        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> {
            delete_generic(TOKEN_1_ID, TOKEN_1, null, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void delete_researcher_succeeds() throws UserNotFoundException, TokenNotFoundException, NotAllowedException {

        /* test */
        delete_generic(TOKEN_1_ID, TOKEN_1, USER_1_USERNAME, USER_1, USER_1_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void delete_developer_succeeds() throws UserNotFoundException, TokenNotFoundException, NotAllowedException {

        /* test */
        delete_generic(TOKEN_2_ID, TOKEN_2, USER_2_USERNAME, USER_2, USER_2_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void delete_dataSteward_fails() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            delete_generic(TOKEN_3_ID, TOKEN_3, USER_3_USERNAME, USER_3, USER_3_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void delete_researcherForeign_fails() {

        /* mock */
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            delete_generic(TOKEN_2_ID, TOKEN_2, USER_2_USERNAME, USER_2, USER_1_PRINCIPAL);
        });
    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    protected void listAll_generic(User user, Principal principal, List<Token> tokens) throws UserNotFoundException {

        /* mock */
        if (user != null) {
            when(userRepository.findByUsername(user.getUsername()))
                    .thenReturn(Optional.of(user));
            when(tokenRepository.findMine(user.getId()))
                    .thenReturn(tokens);
        } else {
            when(userRepository.findByUsername(anyString()))
                    .thenReturn(Optional.empty());
            when(tokenRepository.findMine(anyLong()))
                    .thenReturn(List.of());
        }

        /* test */
        final ResponseEntity<List<TokenBriefDto>> response = tokenEndpoint.listAll(principal);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final List<TokenBriefDto> body = response.getBody();
        assertNotNull(body);
        assertEquals(tokens.size(), body.size());
    }

    protected void create_generic(User user, Principal principal, Token token, List<Token> existingTokens)
            throws UserNotFoundException, TokenNotEligableException {

        /* mock */
        if (user != null) {
            when(userRepository.findByUsername(user.getUsername()))
                    .thenReturn(Optional.of(user));
            when(tokenRepository.findMine(user.getId()))
                    .thenReturn(existingTokens);
        } else {
            when(userRepository.findByUsername(anyString()))
                    .thenReturn(Optional.empty());
            when(tokenRepository.findMine(anyLong()))
                    .thenReturn(List.of());
        }
        when(tokenRepository.save(any(Token.class)))
                .thenReturn(token);

        /* test */
        final ResponseEntity<TokenDto> response = tokenEndpoint.create(principal);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        final TokenDto body = response.getBody();
        assertNotNull(body);
    }

    protected void delete_generic(Long tokenId, Token token, String username, User user, Principal principal)
            throws UserNotFoundException, TokenNotFoundException, NotAllowedException {

        /* mock */
        if (user != null) {
            when(tokenRepository.findById(tokenId))
                    .thenReturn(Optional.of(token));
            when(tokenRepository.findByTokenHash(anyString()))
                    .thenReturn(Optional.of(token));
            when(userRepository.findByUsername(username))
                    .thenReturn(Optional.of(user));
        } else {
            when(tokenRepository.findById(anyLong()))
                    .thenReturn(Optional.empty());
            when(tokenRepository.findByTokenHash(anyString()))
                    .thenReturn(Optional.empty());
            when(userRepository.findByUsername(username))
                    .thenReturn(Optional.empty());
        }
        when(tokenRepository.save(any(Token.class)))
                .thenReturn(token);

        /* test */
        final ResponseEntity<?> response = tokenEndpoint.delete(tokenId, principal);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNull(response.getBody());
    }
}
