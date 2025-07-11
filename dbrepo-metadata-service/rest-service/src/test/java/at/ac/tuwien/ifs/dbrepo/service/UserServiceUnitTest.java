package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.user.UserUpdateDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.user.User;
import at.ac.tuwien.ifs.dbrepo.core.exception.AuthServiceException;
import at.ac.tuwien.ifs.dbrepo.core.exception.UserNotFoundException;
import at.ac.tuwien.ifs.dbrepo.gateway.KeycloakGateway;
import at.ac.tuwien.ifs.dbrepo.repository.UserRepository;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class UserServiceUnitTest extends BaseTest {

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private KeycloakGateway keycloakGateway;

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
    public void findById_succeeds() throws UserNotFoundException {

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
    public void modify_succeeds() throws UserNotFoundException, AuthServiceException {

        /* mock */
        when(userRepository.findById(USER_1_ID))
                .thenReturn(Optional.of(USER_1));
        when(userRepository.save(any(User.class)))
                .thenReturn(USER_1);
        doNothing()
                .when(keycloakGateway)
                .updateUser(any(UUID.class), any(UserUpdateDto.class));

        /* test */
        final User response = userService.modify(USER_1, USER_1_UPDATE_DTO);
        assertEquals(USER_1_ID, response.getId());
        assertEquals(USER_1_USERNAME, response.getUsername());
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
