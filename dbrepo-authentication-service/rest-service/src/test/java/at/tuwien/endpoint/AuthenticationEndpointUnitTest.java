package at.tuwien.endpoint;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.auth.JwtResponseDto;
import at.tuwien.api.auth.LoginRequestDto;
import at.tuwien.api.user.UserDto;
import at.tuwien.endpoints.AuthenticationEndpoint;
import at.tuwien.entities.user.RoleType;
import at.tuwien.entities.user.Token;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.repositories.TokenRepository;
import at.tuwien.repositories.UserRepository;
import at.tuwien.service.AuthenticationService;
import com.auth0.jwt.exceptions.TokenExpiredException;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.StandardPasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class AuthenticationEndpointUnitTest extends BaseUnitTest {

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private TokenRepository tokenRepository;

    @Autowired
    private AuthenticationEndpoint authenticationEndpoint;

    @Test
    public void authenticateUser_anonymous_succeeds() throws UserNotFoundException, UserEmailNotVerifiedException {
        final LoginRequestDto request = LoginRequestDto.builder()
                .username(USER_2_USERNAME)
                .password(USER_2_PASSWORD)
                .build();

        /* mock */
        when(userRepository.findByUsername(USER_2_USERNAME))
                .thenReturn(Optional.of(User.builder()
                        .id(USER_2_ID)
                        .username(USER_2_USERNAME)
                        .email(USER_2_EMAIL)
                        .emailVerified(USER_2_VERIFIED)
                        .roles(List.of(RoleType.ROLE_RESEARCHER))
                        .build()));

        /* test */
        authenticateUser_generic(USER_2, USER_2_PRINCIPAL, request);
    }

    @Test
    public void authenticateUser_anonymousNotVerified_fails() {
        final LoginRequestDto request = LoginRequestDto.builder()
                .username(USER_2_USERNAME)
                .password(USER_2_PASSWORD)
                .build();

        /* mock */
        when(userRepository.findByUsername(USER_2_USERNAME))
                .thenReturn(Optional.of(User.builder()
                        .id(USER_2_ID)
                        .username(USER_2_USERNAME)
                        .email(USER_2_EMAIL)
                        .emailVerified(false)
                        .roles(List.of(RoleType.ROLE_RESEARCHER))
                        .build()));

        /* test */
        assertThrows(UserEmailNotVerifiedException.class, () -> {
            authenticateUser_generic(USER_2, USER_2_PRINCIPAL, request);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"RESEARCHER"})
    public void authenticateUser_researcher_succeeds() throws UserNotFoundException, UserEmailNotVerifiedException {
        final LoginRequestDto request = LoginRequestDto.builder()
                .username(USER_2_USERNAME)
                .password(USER_2_PASSWORD)
                .build();

        /* mock */
        when(userRepository.findByUsername(USER_2_USERNAME))
                .thenReturn(Optional.of(User.builder()
                        .id(USER_2_ID)
                        .username(USER_2_USERNAME)
                        .email(USER_2_EMAIL)
                        .emailVerified(USER_2_VERIFIED)
                        .roles(List.of(RoleType.ROLE_RESEARCHER))
                        .build()));

        /* test */
        authenticateUser_generic(USER_2, USER_2_PRINCIPAL, request);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void authenticateUser_developer_succeeds() throws UserNotFoundException, UserEmailNotVerifiedException {
        final LoginRequestDto request = LoginRequestDto.builder()
                .username(USER_2_USERNAME)
                .password(USER_2_PASSWORD)
                .build();

        /* mock */
        when(userRepository.findByUsername(USER_2_USERNAME))
                .thenReturn(Optional.of(User.builder()
                        .id(USER_2_ID)
                        .username(USER_2_USERNAME)
                        .email(USER_2_EMAIL)
                        .emailVerified(USER_2_VERIFIED)
                        .roles(List.of(RoleType.ROLE_RESEARCHER))
                        .build()));

        /* test */
        authenticateUser_generic(USER_2, USER_2_PRINCIPAL, request);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DATA_STEWARD"})
    public void authenticateUser_dataSteward_succeeds() throws UserNotFoundException, UserEmailNotVerifiedException {
        final LoginRequestDto request = LoginRequestDto.builder()
                .username(USER_2_USERNAME)
                .password(USER_2_PASSWORD)
                .build();

        /* mock */
        when(userRepository.findByUsername(USER_2_USERNAME))
                .thenReturn(Optional.of(User.builder()
                        .id(USER_2_ID)
                        .username(USER_2_USERNAME)
                        .email(USER_2_EMAIL)
                        .emailVerified(USER_2_VERIFIED)
                        .roles(List.of(RoleType.ROLE_DATA_STEWARD))
                        .build()));

        /* test */
        authenticateUser_generic(USER_2, USER_2_PRINCIPAL, request);
    }

    @Test
    public void authenticateUser2_anonymous_succeeds() throws UserNotFoundException, OrcidMalformedException,
            TokenRevokedException {
        final LoginRequestDto request = LoginRequestDto.builder()
                .username(USER_2_USERNAME)
                .password(USER_2_PASSWORD)
                .build();
        final Token token = Token.builder()
                .token(TOKEN_2_TOKEN)
                .tokenHash(TOKEN_2_TOKEN_HASH)
                .creator(USER_2_ID)
                .expires(TOKEN_2_EXPIRES)
                .lastUsed(null)
                .build();

        /* mock */
        when(userRepository.findByUsername(USER_2_USERNAME))
                .thenReturn(Optional.of(User.builder()
                        .id(USER_2_ID)
                        .username(USER_2_USERNAME)
                        .email(USER_2_EMAIL)
                        .emailVerified(USER_2_VERIFIED)
                        .roles(List.of(RoleType.ROLE_DEVELOPER))
                        .build()));
        when(tokenRepository.findByTokenHash(anyString()))
                .thenReturn(Optional.of(token));
        when(tokenRepository.save(any(Token.class)))
                .thenReturn(token);

        /* test */
        authenticateUser2_generic(USER_2, USER_2_PRINCIPAL, TOKEN_2_AUTHORIZATION, request);
    }

    @Test
    public void authenticateUser2_anonymousRevoked_succeeds() {
        final LoginRequestDto request = LoginRequestDto.builder()
                .username(USER_2_USERNAME)
                .password(USER_2_PASSWORD)
                .build();

        /* mock */
        when(userRepository.findByUsername(USER_2_USERNAME))
                .thenReturn(Optional.of(User.builder()
                        .id(USER_2_ID)
                        .username(USER_2_USERNAME)
                        .email(USER_2_EMAIL)
                        .emailVerified(USER_2_VERIFIED)
                        .roles(List.of(RoleType.ROLE_RESEARCHER))
                        .build()));
        when(tokenRepository.findByTokenHash(anyString()))
                .thenReturn(Optional.of(TOKEN_2_EXPIRED));

        /* test */
        assertThrows(TokenExpiredException.class, () -> {
            authenticateUser2_generic(USER_2, USER_2_PRINCIPAL, TOKEN_2_AUTHORIZATION, request);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"RESEARCHER"})
    public void authenticateUser2_researcher_succeeds() throws UserNotFoundException, OrcidMalformedException,
            TokenRevokedException {
        final LoginRequestDto request = LoginRequestDto.builder()
                .username(USER_2_USERNAME)
                .password(USER_2_PASSWORD)
                .build();
        final Token token = Token.builder()
                .token(TOKEN_2_TOKEN)
                .tokenHash(TOKEN_2_TOKEN_HASH)
                .creator(USER_2_ID)
                .expires(TOKEN_2_EXPIRES)
                .lastUsed(null)
                .build();

        /* mock */
        when(userRepository.findByUsername(USER_2_USERNAME))
                .thenReturn(Optional.of(User.builder()
                        .id(USER_2_ID)
                        .username(USER_2_USERNAME)
                        .email(USER_2_EMAIL)
                        .emailVerified(USER_2_VERIFIED)
                        .roles(List.of(RoleType.ROLE_RESEARCHER))
                        .build()));
        when(tokenRepository.findByTokenHash(anyString()))
                .thenReturn(Optional.of(token));
        when(tokenRepository.save(any(Token.class)))
                .thenReturn(token);

        /* test */
        authenticateUser2_generic(USER_2, USER_2_PRINCIPAL, TOKEN_2_AUTHORIZATION, request);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void authenticateUser2_developer_succeeds() throws UserNotFoundException, OrcidMalformedException,
            TokenRevokedException {
        final LoginRequestDto request = LoginRequestDto.builder()
                .username(USER_2_USERNAME)
                .password(USER_2_PASSWORD)
                .build();
        final Token token = Token.builder()
                .token(TOKEN_2_TOKEN)
                .tokenHash(TOKEN_2_TOKEN_HASH)
                .creator(USER_2_ID)
                .expires(TOKEN_2_EXPIRES)
                .lastUsed(null)
                .build();

        /* mock */
        when(userRepository.findByUsername(USER_2_USERNAME))
                .thenReturn(Optional.of(User.builder()
                        .id(USER_2_ID)
                        .username(USER_2_USERNAME)
                        .email(USER_2_EMAIL)
                        .emailVerified(USER_2_VERIFIED)
                        .roles(List.of(RoleType.ROLE_DEVELOPER))
                        .build()));
        when(tokenRepository.findByTokenHash(anyString()))
                .thenReturn(Optional.of(token));
        when(tokenRepository.save(any(Token.class)))
                .thenReturn(token);

        /* test */
        authenticateUser2_generic(USER_2, USER_2_PRINCIPAL, TOKEN_2_AUTHORIZATION, request);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DATA_STEWARD"})
    public void authenticateUser2_dataSteward_succeeds() throws UserNotFoundException, OrcidMalformedException,
            TokenRevokedException {
        final LoginRequestDto request = LoginRequestDto.builder()
                .username(USER_2_USERNAME)
                .password(USER_2_PASSWORD)
                .build();
        final Token token = Token.builder()
                .token(TOKEN_2_TOKEN)
                .tokenHash(TOKEN_2_TOKEN_HASH)
                .creator(USER_2_ID)
                .expires(TOKEN_2_EXPIRES)
                .lastUsed(null)
                .build();

        /* mock */
        when(userRepository.findByUsername(USER_2_USERNAME))
                .thenReturn(Optional.of(User.builder()
                        .id(USER_2_ID)
                        .username(USER_2_USERNAME)
                        .email(USER_2_EMAIL)
                        .emailVerified(USER_2_VERIFIED)
                        .roles(List.of(RoleType.ROLE_DATA_STEWARD))
                        .build()));
        when(tokenRepository.findByTokenHash(anyString()))
                .thenReturn(Optional.of(token));
        when(tokenRepository.save(any(Token.class)))
                .thenReturn(token);

        /* test */
        authenticateUser2_generic(USER_2, USER_2_PRINCIPAL, TOKEN_2_AUTHORIZATION, request);
    }

    @Test
    public void reAuthenticateUser_anonymous_fails() {

        /* test */
        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> {
            reAuthenticateUser_generic(USER_2_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void reAuthenticateUser_researcher_succeeds() {

        /* test */
        reAuthenticateUser_generic(USER_1_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void reAuthenticateUser_developer_succeeds() {

        /* test */
        reAuthenticateUser_generic(USER_2_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void reAuthenticateUser_dataSteward_succeeds() {

        /* test */
        reAuthenticateUser_generic(USER_3_PRINCIPAL);
    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    protected void authenticateUser_generic(User user, Principal principal, LoginRequestDto data) throws UserNotFoundException,
            UserEmailNotVerifiedException {

        /* mock */
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenReturn((Authentication) principal);

        /* test */
        final ResponseEntity<JwtResponseDto> response = authenticationEndpoint.authenticateUser(data);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        final JwtResponseDto body = response.getBody();
        assertNotNull(body);
        assertEquals(user.getId(), body.getId());
        assertEquals(user.getUsername(), body.getUsername());
        assertEquals(user.getEmail(), body.getEmail());
        assertEquals(user.getRoles().stream().map(Enum::name).collect(Collectors.toList()), body.getRoles());
        assertNotNull(body.getToken());
    }

    protected void authenticateUser2_generic(User user, Principal principal, String authorization, LoginRequestDto data) throws UserNotFoundException,
            OrcidMalformedException, TokenRevokedException {

        /* mock */
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenReturn((Authentication) principal);

        /* test */
        final ResponseEntity<UserDto> response = authenticationEndpoint.authenticateUser(principal, authorization);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        final UserDto body = response.getBody();
        assertNotNull(body);
        assertEquals(user.getId(), body.getId());
        assertEquals(user.getUsername(), body.getUsername());
        assertEquals(user.getEmail(), body.getEmail());
    }

    protected void reAuthenticateUser_generic(Principal principal) {

        /* test */
        final ResponseEntity<JwtResponseDto> response = authenticationEndpoint.reAuthenticateUser(principal);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final JwtResponseDto body = response.getBody();
        assertNotNull(body);
    }

}
