package at.tuwien.service;

import at.tuwien.api.auth.SignupRequestDto;
import at.tuwien.api.user.UserPasswordDto;
import at.tuwien.api.user.UserUpdateDto;
import at.tuwien.entities.user.User;
import at.tuwien.exception.EmailExistsException;
import at.tuwien.exception.UserExistsException;
import at.tuwien.exception.UserNotFoundException;
import at.tuwien.repository.UserRepository;
import at.tuwien.test.AbstractUnitTest;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Log4j2
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class UserServicePersistenceTest extends AbstractUnitTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @BeforeEach
    public void beforeEach() {
        genesis();
        /* metadata database */
        userRepository.save(USER_1);
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
    public void findAll_succeeds() {

        /* test */
        final List<User> response = userService.findAll();
        assertEquals(1, response.size());
    }

    @Test
    public void create_succeeds() throws UserExistsException, UserNotFoundException, EmailExistsException {
        final SignupRequestDto request = SignupRequestDto.builder()
                .username(USER_2_USERNAME)
                .password(USER_2_PASSWORD)
                .email(USER_2_EMAIL)
                .build();

        /* test */
        final User response = userService.create(request, USER_2_ID);
        assertEquals(USER_2_USERNAME, response.getUsername());
    }

    @Test
    public void modify_succeeds() {
        final UserUpdateDto request = UserUpdateDto.builder()
                .firstname(USER_1_FIRSTNAME)
                .lastname(USER_1_LASTNAME)
                .affiliation("NASA")
                .orcid(null)
                .theme("dark")
                .language("de")
                .build();

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
    public void updatePassword_succeeds() {
        final UserPasswordDto request = UserPasswordDto.builder()
                .password(USER_3_PASSWORD)
                .build();

        /* mock */
        final User user = userService.create(SignupRequestDto.builder()
                .username(USER_3_USERNAME)
                .password(USER_3_PASSWORD)
                .email(USER_3_EMAIL)
                .build(), USER_3_ID);

        /* test */
        userService.updatePassword(user, request);
    }

    @Test
    public void find_succeeds() throws UserNotFoundException {

        /* test */
        final User user = userService.findById(USER_1_ID);
        assertEquals(USER_1_USERNAME, user.getUsername());
    }

    @Test
    public void find_notFound_fails() {

        /* test */
        assertThrows(UserNotFoundException.class, () -> {
            userService.findById(USER_2_ID);
        });
    }

    @Test
    public void validateUsernameNotExists_succeeds() throws UserExistsException {

        /* test */
        userService.validateUsernameNotExists(USER_2_USERNAME);
    }

    @Test
    public void validateUsernameNotExists_fails() {

        /* test */
        assertThrows(UserExistsException.class, () -> {
            userService.validateUsernameNotExists(USER_1_USERNAME);
        });
    }

    @Test
    public void validateEmailNotExists_succeeds() throws EmailExistsException {

        /* test */
        userService.validateEmailNotExists(USER_2_EMAIL);
    }

    @Test
    public void validateEmailNotExists_fails() {

        /* test */
        assertThrows(EmailExistsException.class, () -> {
            userService.validateEmailNotExists(USER_1_EMAIL);
        });
    }
}
