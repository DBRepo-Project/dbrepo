package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.entities.user.User;
import at.tuwien.exception.UserNotFoundException;
import at.tuwien.repository.mdb.RealmRepository;
import at.tuwien.repository.mdb.UserRepository;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Log4j2
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest
@ExtendWith(SpringExtension.class)
@MockAmqp
@MockOpensearch
public class UserServiceIntegrationTest extends BaseUnitTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RealmRepository realmRepository;

    @Autowired
    private UserService userService;

    @BeforeEach
    public void beforeEach() {
        realmRepository.save(REALM_DBREPO);
        userRepository.save(USER_1_SIMPLE);
    }

    @Test
    public void findByUsername_succeeds() throws UserNotFoundException {

        /* test */
        final User response = userService.findByUsername(USER_1_USERNAME);
        assertEquals(USER_1_ID, response.getId());
        assertEquals(USER_1_USERNAME, response.getUsername());
    }

    @Test
    public void findByUsername_fails() {

        /* test */
        assertThrows(UserNotFoundException.class, () -> {
            userService.findByUsername(USER_2_USERNAME);
        });
    }

    @Test
    public void find_succeeds() throws UserNotFoundException {

        /* test */
        final User response = userService.find(USER_1_ID);
        assertEquals(USER_1_ID, response.getId());
    }

    @Test
    public void find_fails() {

        /* test */
        assertThrows(UserNotFoundException.class, () -> {
            userService.find(UUID.fromString("deadbeef-20bc-4191-acb2-77a3afcae32f"));
        });
    }

}
