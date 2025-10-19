package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.user.UserUpdateDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.user.User;
import at.ac.tuwien.ifs.dbrepo.core.exception.AuthServiceException;
import at.ac.tuwien.ifs.dbrepo.core.exception.UserNotFoundException;
import at.ac.tuwien.ifs.dbrepo.gateway.KeycloakGateway;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doNothing;

@Slf4j
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class UserServicePersistenceTest extends BaseTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @MockBean
    private KeycloakGateway keycloakGateway;

    @BeforeEach
    public void beforeEach() {
        /* metadata database */
        userRepository.saveAll(List.of(USER_1, USER_LOCAL));
    }

    @Test
    public void findByUsername_succeeds() throws UserNotFoundException {

        /* test */
        final User response = userService.findByUsername(USER_1_USERNAME);
        assertEquals(USER_1_ID, response.getId());
        assertEquals(USER_1_USERNAME, response.getUsername());
    }

    @Test
    public void findAllInternalUsers_succeeds() {

        /* test */
        final List<User> response = userService.findAllInternalUsers();
        assertEquals(1, response.size());
        final User user0 = response.get(0);
        assertEquals(USER_LOCAL_ADMIN_ID, user0.getId());
    }

    @Test
    public void findByUsername_fails() {

        /* test */
        assertThrows(UserNotFoundException.class, () -> {
            userService.findByUsername(USER_2_USERNAME);
        });
    }

    @Test
    public void findAll_succeeds() {

        /* test */
        final List<User> response = userService.findAll();
        assertEquals(2, response.size());
    }

    @Test
    public void modify_succeeds() throws UserNotFoundException, AuthServiceException {
        final UserUpdateDto request = UserUpdateDto.builder()
                .firstname(USER_1_FIRSTNAME)
                .lastname(USER_1_LASTNAME)
                .affiliation("NASA")
                .orcid(null)
                .theme("dark")
                .language("de")
                .build();

        /* mock */
        doNothing()
                .when(keycloakGateway)
                .updateUser(USER_1_ID, request);

        /* test */
        final User response = userService.modify(USER_1, request);
        assertEquals(USER_1_ID, response.getId());
        assertEquals(USER_1_FIRSTNAME, response.getFirstname());
        assertEquals(USER_1_LASTNAME, response.getLastname());
        assertEquals("dark", response.getTheme());
        assertEquals("de", response.getLanguage());
        assertEquals("NASA", response.getAffiliation());
        assertNull(response.getOrcid());
    }

    @Test
    public void findById_succeeds() throws UserNotFoundException {

        /* test */
        final User user = userService.findById(USER_1_ID);
        assertEquals(USER_1_USERNAME, user.getUsername());
    }

    @Test
    public void findById_notFound_fails() {

        /* test */
        assertThrows(UserNotFoundException.class, () -> {
            userService.findById(USER_2_ID);
        });
    }
}
