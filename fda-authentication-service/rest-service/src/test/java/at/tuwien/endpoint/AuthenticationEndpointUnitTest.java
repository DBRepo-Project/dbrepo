package at.tuwien.endpoint;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.auth.JwtResponseDto;
import at.tuwien.api.auth.LoginRequestDto;
import at.tuwien.api.user.UserDto;
import at.tuwien.endpoints.AuthenticationEndpoint;
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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.StandardPasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.security.Principal;
import java.util.HashMap;
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
                .thenReturn(Optional.of(USER_2));
        assert USER_2.getEmailVerified();

        /* test */
        authenticateUser_generic(USER_2, USER_2_PRINCIPAL, request);
    }

    @Test
    public void authenticateUser_anonymousNotVerified_fails() {
        final LoginRequestDto request = LoginRequestDto.builder()
                .username(USER_1_USERNAME)
                .password(USER_1_PASSWORD)
                .build();

        /* mock */
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));

        /* test */
        assertThrows(UserEmailNotVerifiedException.class, () -> {
            authenticateUser_generic(USER_1, USER_1_PRINCIPAL, request);
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
                .thenReturn(Optional.of(USER_2));
        assert USER_2.getEmailVerified();

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
                .thenReturn(Optional.of(USER_2));
        assert USER_2.getEmailVerified();

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
                .thenReturn(Optional.of(USER_2));
        assert USER_2.getEmailVerified();

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

        /* mock */
        when(userRepository.findByUsername(USER_2_USERNAME))
                .thenReturn(Optional.of(USER_2));
        when(tokenRepository.findByTokenHash(anyString()))
                .thenReturn(Optional.of(TOKEN_2));
        when(tokenRepository.save(any(Token.class)))
                .thenReturn(TOKEN_2);

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
                .thenReturn(Optional.of(USER_2));
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

        /* mock */
        when(userRepository.findByUsername(USER_2_USERNAME))
                .thenReturn(Optional.of(USER_2));
        when(tokenRepository.findByTokenHash(anyString()))
                .thenReturn(Optional.of(TOKEN_2));
        when(tokenRepository.save(any(Token.class)))
                .thenReturn(TOKEN_2);

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

        /* mock */
        when(userRepository.findByUsername(USER_2_USERNAME))
                .thenReturn(Optional.of(USER_2));
        when(tokenRepository.findByTokenHash(anyString()))
                .thenReturn(Optional.of(TOKEN_2));
        when(tokenRepository.save(any(Token.class)))
                .thenReturn(TOKEN_2);

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

        /* mock */
        when(userRepository.findByUsername(USER_2_USERNAME))
                .thenReturn(Optional.of(USER_2));
        when(tokenRepository.findByTokenHash(anyString()))
                .thenReturn(Optional.of(TOKEN_2));
        when(tokenRepository.save(any(Token.class)))
                .thenReturn(TOKEN_2);

        /* test */
        authenticateUser2_generic(USER_2, USER_2_PRINCIPAL, TOKEN_2_AUTHORIZATION, request);
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
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
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
        assertEquals(user.getRoles().stream().map(Enum::name).collect(Collectors.toList()), body.getRoles());
    }

}
