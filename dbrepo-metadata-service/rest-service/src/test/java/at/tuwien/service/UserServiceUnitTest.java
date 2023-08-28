package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.api.user.UserBriefDto;
import at.tuwien.api.user.UserDto;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.gateway.KeycloakGateway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@MockAmqp
@MockOpensearch
public class UserServiceUnitTest extends BaseUnitTest {

    @MockBean
    private KeycloakGateway keycloakGateway;

    @Autowired
    private UserService userService;

    @Test
    public void findByUsername_succeeds() throws UserNotFoundException {

        /* test */
        final User response = userService.findByUsername(USER_1_USERNAME);
        assertEquals(USER_1_ID, response.getId());
        assertEquals(USER_1_USERNAME, response.getUsername());
    }

    @Test
    public void find_succeeds() throws UserNotFoundException {

        /* test */
        final User response = userService.find(USER_1_ID);
        assertEquals(USER_1_ID, response.getId());
        assertEquals(USER_1_USERNAME, response.getUsername());
    }

    @Test
    public void findAll_succeeds() throws UserNotFoundException {

        /* test */
        final List<User> response = userService.findAll();
        assertEquals(1, response.size());
    }

    @Test
    public void create_succeeds() throws UserNotFoundException, KeycloakRemoteException, AccessDeniedException,
            UserAlreadyExistsException {

        /* mock */
        doNothing()
                .when(keycloakGateway)
                .createUser(USER_1_KEYCLOAK_SIGNUP_REQUEST);
        when(keycloakGateway.findByUsername(USER_1_USERNAME))
                .thenReturn(USER_1_KEYCLOAK_DTO);

        /* test */
        final User response = userService.create(USER_1_SIGNUP_REQUEST_DTO);
        assertEquals(USER_1_ID, response.getId());
        assertEquals(USER_1_USERNAME, response.getUsername());
    }

    @Test
    public void modify_succeeds() throws UserNotFoundException {

        /* test */
        final User response = userService.modify(USER_1_ID, USER_1_UPDATE_DTO);
        assertEquals(USER_1_ID, response.getId());
        assertEquals(USER_1_USERNAME, response.getUsername());
    }

    @Test
    public void modify_notExists_succeeds() {

        /* test */
        assertThrows(KeycloakRemoteException.class, () -> {
            userService.modify(USER_1_ID, USER_1_UPDATE_DTO);
        });
    }

    @Test
    public void toggleTheme_succeeds() throws UserNotFoundException {

        /* test */
        final User response = userService.toggleTheme(USER_1_ID, USER_1_THEME_SET_DTO);
        assertEquals(USER_1_ID, response.getId());
        assertEquals(USER_1_USERNAME, response.getUsername());
        assertEquals(USER_1_THEME_DARK, response.getThemeDark());
    }

    @Test
    public void updatePassword_succeeds() throws KeycloakRemoteException, AccessDeniedException {

        /* mock */
        doNothing()
                .when(keycloakGateway)
                .updateUserCredentials(USER_1_ID, USER_1_PASSWORD_DTO);

        /* test */
        userService.updatePassword(USER_1_ID, USER_1_PASSWORD_DTO);
    }

    @Test
    public void findByUsername_fails() {

        /* test */
        assertThrows(UserNotFoundException.class, () -> {
            userService.findByUsername(USER_1_USERNAME);
        });
    }

    @Test
    public void find_fails() {

        /* test */
        assertThrows(UserNotFoundException.class, () -> {
            userService.find(USER_1_ID);
        });
    }


}
