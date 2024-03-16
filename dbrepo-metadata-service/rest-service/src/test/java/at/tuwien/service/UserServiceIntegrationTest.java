package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockListeners;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.api.auth.SignupRequestDto;
import at.tuwien.api.user.*;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.repository.mdb.UserRepository;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Log4j2
@Testcontainers
@EnableAutoConfiguration(exclude = RabbitAutoConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest
@ExtendWith(SpringExtension.class)
@MockAmqp
@MockListeners
@MockOpensearch
public class UserServiceIntegrationTest extends BaseUnitTest {

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
    public void findAll_succeeds() throws KeycloakRemoteException, AccessDeniedException {

        /* test */
        final List<User> response = userService.findAll();
        assertEquals(1, response.size());
    }

    @Test
    public void create_succeeds() throws UserAlreadyExistsException, UserNotFoundException, KeycloakRemoteException,
            AccessDeniedException, UserEmailAlreadyExistsException {
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
    @Transactional
    public void modify_succeeds() throws UserNotFoundException {
        final UserUpdateDto request = UserUpdateDto.builder()
                .firstname(USER_1_FIRSTNAME)
                .lastname(USER_1_LASTNAME)
                .affiliation("NASA")
                .orcid(null)
                .build();

        /* test */
        final User response = userService.modify(USER_1_ID, request);
        assertEquals(USER_1_ID, response.getId());
        assertEquals(USER_1_FIRSTNAME, response.getFirstname());
        assertEquals(USER_1_LASTNAME, response.getLastname());
        assertEquals("NASA", response.getAffiliation());
        assertNull(response.getOrcid());
    }

    @Test
    public void modify_notFound_fails() {
        final UserUpdateDto request = UserUpdateDto.builder()
                .firstname(USER_2_FIRSTNAME)
                .lastname(USER_2_LASTNAME)
                .affiliation(USER_2_AFFILIATION)
                .orcid(USER_2_ORCID_URL)
                .build();

        /* test */
        assertThrows(UserNotFoundException.class, () -> {
            userService.modify(USER_2_ID, request);
        });
    }

    @Test
    public void updatePassword_succeeds() throws UserNotFoundException {
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
        userService.updatePassword(user.getId(), request);
    }

    @Test
    public void updatePassword_notFound_fails() {
        final UserPasswordDto request = UserPasswordDto.builder()
                .password(USER_1_PASSWORD)
                .build();

        /* test */
        assertThrows(UserNotFoundException.class, () -> {
            userService.updatePassword(USER_2_ID, request);
        });
    }

    @Test
    @Transactional
    public void toggleTheme_succeeds() throws UserNotFoundException {

        /* test */
        final User response = userService.toggleTheme(USER_1_ID, USER_THEME_DARK_DTO);
        assertEquals(USER_THEME_DARK_DTO.getTheme(), response.getTheme());
    }

    @Test
    public void toggleTheme_fails() {

        /* test */
        assertThrows(UserNotFoundException.class, () -> {
            userService.toggleTheme(USER_2_ID, USER_THEME_DARK_DTO);
        });
    }

    @Test
    public void find_succeeds() throws UserNotFoundException {

        /* test */
        final User user = userService.find(USER_1_ID);
        assertEquals(USER_1_USERNAME, user.getUsername());
    }

    @Test
    public void find_notFound_fails() {

        /* test */
        assertThrows(UserNotFoundException.class, () -> {
            userService.find(USER_2_ID);
        });
    }

    @Test
    public void validateUsernameNotExists_succeeds() throws UserAlreadyExistsException {

        /* test */
        userService.validateUsernameNotExists(USER_2_USERNAME);
    }

    @Test
    public void validateUsernameNotExists_fails() {

        /* test */
        assertThrows(UserAlreadyExistsException.class, () -> {
            userService.validateUsernameNotExists(USER_1_USERNAME);
        });
    }

    @Test
    public void validateEmailNotExists_succeeds() throws UserEmailAlreadyExistsException {

        /* test */
        userService.validateEmailNotExists(USER_2_EMAIL);
    }

    @Test
    public void validateEmailNotExists_fails() {

        /* test */
        assertThrows(UserEmailAlreadyExistsException.class, () -> {
            userService.validateEmailNotExists(USER_1_EMAIL);
        });
    }
}
