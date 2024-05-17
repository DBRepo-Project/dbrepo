package at.tuwien.service;

import at.tuwien.test.AbstractUnitTest;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.gateway.KeycloakGateway;
import at.tuwien.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class UserServiceUnitTest extends AbstractUnitTest {

    @MockBean
    private KeycloakGateway keycloakGateway;

    @MockBean
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Test
    public void findByUsername_succeeds() throws UserNotFoundException {

        /* mock */
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));

        /* test */
        final User response = userService.findByUsername(USER_1_USERNAME);
        assertEquals(USER_1_ID, response.getId());
        assertEquals(USER_1_USERNAME, response.getUsername());
    }

    @Test
    public void find_succeeds() throws UserNotFoundException {

        /* mock */
        when(userRepository.findById(USER_1_ID))
                .thenReturn(Optional.of(USER_1));

        /* test */
        final User response = userService.findById(USER_1_ID);
        assertEquals(USER_1_ID, response.getId());
        assertEquals(USER_1_USERNAME, response.getUsername());
    }

    @Test
    public void findAll_succeeds() {

        /* mock */
        when(userRepository.findAll())
                .thenReturn(List.of(USER_1, USER_2));

        /* test */
        final List<User> response = userService.findAll();
        assertEquals(2, response.size());
    }

    @Test
    public void create_succeeds() throws UserNotFoundException, UserExistsException, EmailExistsException,
            ServiceException, ServiceConnectionException {

        /* mock */
        when(userRepository.findById(USER_1_ID))
                .thenReturn(Optional.of(USER_1));
        when(userRepository.save(any(User.class)))
                .thenReturn(USER_1);
        doNothing()
                .when(keycloakGateway)
                .createUser(USER_1_KEYCLOAK_SIGNUP_REQUEST);
        when(keycloakGateway.findByUsername(USER_1_USERNAME))
                .thenReturn(USER_1_KEYCLOAK_DTO);

        /* test */
        final User response = userService.create(USER_1_SIGNUP_REQUEST_DTO, USER_1_ID);
        assertEquals(USER_1_ID, response.getId());
        assertEquals(USER_1_USERNAME, response.getUsername());
    }

    @Test
    public void modify_succeeds() {

        /* mock */
        when(userRepository.findById(USER_1_ID))
                .thenReturn(Optional.of(USER_1));
        when(userRepository.save(any(User.class)))
                .thenReturn(USER_1);

        /* test */
        final User response = userService.modify(USER_1, USER_1_UPDATE_DTO);
        assertEquals(USER_1_ID, response.getId());
        assertEquals(USER_1_USERNAME, response.getUsername());
    }

    @Test
    public void updatePassword_succeeds() throws ServiceException, ServiceConnectionException {

        /* mock */
        doNothing()
                .when(keycloakGateway)
                .updateUserCredentials(USER_1_ID, USER_1_PASSWORD_DTO);
        when(userRepository.findById(USER_1_ID))
                .thenReturn(Optional.of(USER_1));
        when(userRepository.save(any(User.class)))
                .thenReturn(USER_1);

        /* test */
        userService.updatePassword(USER_1, USER_1_PASSWORD_DTO);
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
            userService.findById(USER_1_ID);
        });
    }


}
