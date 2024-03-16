package at.tuwien.endpoints;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.api.auth.SignupRequestDto;
import at.tuwien.api.user.*;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.service.AuthenticationService;
import at.tuwien.service.MessageQueueService;
import at.tuwien.service.UserService;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Log4j2
@EnableAutoConfiguration(exclude = RabbitAutoConfiguration.class)
@SpringBootTest
@ExtendWith(SpringExtension.class)
@MockAmqp
@MockOpensearch
public class UserEndpointUnitTest extends BaseUnitTest {

    @MockBean
    private UserService userService;

    @MockBean
    private MessageQueueService messageQueueService;

    @MockBean
    private AuthenticationService authenticationService;

    @Autowired
    private UserEndpoint userEndpoint;

    @Test
    @WithAnonymousUser
    public void findAll_anonymous_succeeds() {

        /* test */
        findAll_generic();
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void findAll_noRole_succeeds() {

        /* test */
        findAll_generic();
    }

    @Test
    @WithAnonymousUser
    public void create_anonymous_succeeds() throws UserNotFoundException, UserEmailAlreadyExistsException,
            UserAlreadyExistsException, KeycloakRemoteException,
            at.tuwien.exception.AccessDeniedException, BrokerRemoteException, BrokerVirtualHostModificationException {
        final SignupRequestDto request = SignupRequestDto.builder()
                .email(USER_1_EMAIL)
                .username(USER_1_USERNAME)
                .password(USER_1_PASSWORD)
                .build();

        /* test */
        create_generic(request, USER_1, USER_1_KEYCLOAK_DTO, USER_1_ID);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void create_isAuthenticated_fails() {
        final SignupRequestDto request = SignupRequestDto.builder()
                .email(USER_2_EMAIL)
                .username(USER_2_USERNAME)
                .password(USER_2_PASSWORD)
                .build();

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            create_generic(request, null, null, null);
        });
    }

    @Test
    @WithAnonymousUser
    public void find_anonymous_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            find_generic(USER_1_ID, USER_1, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void find_self_succeeds() throws UserNotFoundException, NotAllowedException, KeycloakRemoteException,
            at.tuwien.exception.AccessDeniedException {

        /* test */
        find_generic(USER_1_ID, USER_1, USER_1_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void find_foreign_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            find_generic(USER_2_ID, USER_2, USER_1_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, authorities = {"find-user"})
    public void find_hasRoleForeign_succeeds() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            find_generic(USER_2_ID, USER_2, USER_3_PRINCIPAL);
        });
    }

    @Test
    @WithAnonymousUser
    public void modify_anonymous_fails() {
        final UserUpdateDto request = UserUpdateDto.builder()
                .firstname(USER_1_FIRSTNAME)
                .lastname(USER_1_LASTNAME)
                .affiliation(USER_1_AFFILIATION)
                .orcid(USER_1_ORCID)
                .build();

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            modify_generic(USER_1_ID, USER_1, null, request);
        });
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void modify_noRole_fails() {
        final UserUpdateDto request = UserUpdateDto.builder()
                .firstname(USER_1_FIRSTNAME)
                .lastname(USER_1_LASTNAME)
                .affiliation(USER_1_AFFILIATION)
                .orcid(USER_1_ORCID)
                .build();

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            modify_generic(USER_1_ID, USER_1, USER_4_PRINCIPAL, request);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"modify-user-information"})
    public void modify_hasRoleForeign_fails() {
        final UserUpdateDto request = UserUpdateDto.builder()
                .firstname(USER_1_FIRSTNAME)
                .lastname(USER_1_LASTNAME)
                .affiliation(USER_1_AFFILIATION)
                .orcid(USER_1_ORCID)
                .build();

        /* test */
        assertThrows(ForeignUserException.class, () -> {
            modify_generic(USER_1_ID, USER_1, USER_2_PRINCIPAL, request);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"modify-user-information"})
    public void modify_succeeds() throws UserNotFoundException, ForeignUserException, UserAttributeNotFoundException,
            KeycloakRemoteException, at.tuwien.exception.AccessDeniedException, QueryMalformedException,
            DatabaseMalformedException {
        final UserUpdateDto request = UserUpdateDto.builder()
                .firstname(USER_1_FIRSTNAME)
                .lastname(USER_1_LASTNAME)
                .affiliation(USER_1_AFFILIATION)
                .orcid(USER_1_ORCID)
                .build();

        /* test */
        modify_generic(USER_1_ID, USER_1, USER_1_PRINCIPAL, request);
    }

    @Test
    @WithAnonymousUser
    public void theme_anonymous_fails() {
        final UserThemeSetDto request = UserThemeSetDto.builder()
                .theme(USER_1_THEME)
                .build();

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            theme_generic(USER_1_ID, USER_1, null, request);
        });
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void theme_noRole_fails() {
        final UserThemeSetDto request = UserThemeSetDto.builder()
                .theme(USER_1_THEME)
                .build();

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            theme_generic(USER_4_ID, USER_4, USER_4_PRINCIPAL, request);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"modify-user-theme"})
    public void theme_hasRoleForeign_fails() {
        final UserThemeSetDto request = UserThemeSetDto.builder()
                .theme(USER_1_THEME)
                .build();

        /* test */
        assertThrows(ForeignUserException.class, () -> {
            theme_generic(USER_1_ID, USER_1, USER_2_PRINCIPAL, request);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"modify-user-theme"})
    public void theme_succeeds() throws UserNotFoundException, ForeignUserException {
        final UserThemeSetDto request = UserThemeSetDto.builder()
                .theme(USER_1_THEME)
                .build();

        /* test */
        theme_generic(USER_1_ID, USER_1, USER_1_PRINCIPAL, request);
    }

    @Test
    @WithAnonymousUser
    public void password_anonymous_fails() {
        final UserPasswordDto request = UserPasswordDto.builder()
                .password(USER_1_PASSWORD)
                .build();

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            password_generic(USER_1_ID, USER_1, null, request);
        });
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void password_noRoleForeign_fails() {
        final UserPasswordDto request = UserPasswordDto.builder()
                .password(USER_1_PASSWORD)
                .build();

        /* test */
        assertThrows(ForeignUserException.class, () -> {
            password_generic(USER_1_ID, USER_1, USER_4_PRINCIPAL, request);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void password_succeeds() throws UserNotFoundException, ForeignUserException, KeycloakRemoteException,
            at.tuwien.exception.AccessDeniedException, QueryMalformedException, DatabaseMalformedException {
        final UserPasswordDto request = UserPasswordDto.builder()
                .password(USER_1_PASSWORD)
                .build();

        /* test */
        password_generic(USER_1_ID, USER_1, USER_1_PRINCIPAL, request);
    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    protected void findAll_generic() {

        /* mock */
        when(userService.findAll())
                .thenReturn(List.of(USER_1, USER_2));

        /* test */
        final ResponseEntity<List<UserBriefDto>> response = userEndpoint.findAll();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final List<UserBriefDto> body = response.getBody();
        assertNotNull(body);
        assertEquals(2, body.size());
    }

    protected void create_generic(SignupRequestDto data, User user, at.tuwien.api.keycloak.UserDto userDto, UUID id)
            throws UserEmailAlreadyExistsException, UserAlreadyExistsException, UserNotFoundException,
            KeycloakRemoteException, AccessDeniedException, BrokerRemoteException,
            BrokerVirtualHostModificationException {

        /* mock */
        when(userService.create(data, id))
                .thenReturn(user);
        doNothing()
                .when(messageQueueService)
                .createUser(anyString(), anyString());
        when(authenticationService.findByUsername(data.getUsername()))
                .thenReturn(userDto);
        doNothing()
                .when(authenticationService)
                .create(any(SignupRequestDto.class));

        /* test */
        final ResponseEntity<UserBriefDto> response = userEndpoint.create(data);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        final UserBriefDto body = response.getBody();
        assertNotNull(body);
    }

    protected void find_generic(UUID id, User user, Principal principal) throws UserNotFoundException,
            NotAllowedException, KeycloakRemoteException, at.tuwien.exception.AccessDeniedException {

        /* mock */
        if (user != null) {
            when(userService.find(id))
                    .thenReturn(user);
        } else {
            doThrow(UserNotFoundException.class)
                    .when(userService)
                    .find(id);
        }

        /* test */
        final ResponseEntity<UserDto> response = userEndpoint.find(id, principal);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final UserDto body = response.getBody();
        assertNotNull(body);
    }

    protected void modify_generic(UUID id, User user, Principal principal, UserUpdateDto data)
            throws UserNotFoundException, ForeignUserException, UserAttributeNotFoundException, KeycloakRemoteException,
            at.tuwien.exception.AccessDeniedException, QueryMalformedException, DatabaseMalformedException {

        /* mock */
        if (user != null) {
            when(userService.find(id))
                    .thenReturn(user);
        } else {
            doThrow(UserNotFoundException.class)
                    .when(userService)
                    .find(id);
        }
        when(userService.modify(id, data))
                .thenReturn(user);

        /* test */
        final ResponseEntity<UserDto> response = userEndpoint.modify(id, data, principal);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        final UserDto body = response.getBody();
        assertNotNull(body);
    }

    protected void theme_generic(UUID id, User user, Principal principal, UserThemeSetDto data)
            throws UserNotFoundException, ForeignUserException {

        /* mock */
        if (user != null) {
            when(userService.find(id))
                    .thenReturn(user);
        } else {
            doThrow(UserNotFoundException.class)
                    .when(userService)
                    .find(id);
        }
        when(userService.toggleTheme(id, data))
                .thenReturn(user);

        /* test */
        final ResponseEntity<UserDto> response = userEndpoint.theme(id, data, principal);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        final UserDto body = response.getBody();
        assertNotNull(body);
    }

    protected void password_generic(UUID id, User user, Principal principal, UserPasswordDto data)
            throws UserNotFoundException, ForeignUserException, KeycloakRemoteException,
            at.tuwien.exception.AccessDeniedException, QueryMalformedException, DatabaseMalformedException {

        /* mock */
        if (user != null) {
            when(userService.find(id))
                    .thenReturn(user);
        } else {
            doThrow(UserNotFoundException.class)
                    .when(userService)
                    .find(id);
        }
        doNothing()
                .when(userService)
                .updatePassword(id, data);

        /* test */
        final ResponseEntity<?> response = userEndpoint.password(id, data, principal);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    }
}
