package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.auth.SignupRequestDto;
import at.tuwien.api.user.UserPasswordDto;
import at.tuwien.api.user.UserThemeSetDto;
import at.tuwien.api.user.UserUpdateDto;
import at.tuwien.config.IndexConfig;
import at.tuwien.entities.user.Role;
import at.tuwien.entities.user.User;
import at.tuwien.entities.user.UserAttribute;
import at.tuwien.exception.*;
import at.tuwien.repository.mdb.RealmRepository;
import at.tuwien.repository.mdb.RoleRepository;
import at.tuwien.repository.mdb.UserRepository;
import at.tuwien.repository.sdb.UserIdxRepository;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Log4j2
@EnableAutoConfiguration(exclude = RabbitAutoConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class UserServiceIntegrationTest extends BaseUnitTest {

    @MockBean
    private IndexConfig indexConfig;

    @MockBean
    private UserIdxRepository userIdxRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RealmRepository realmRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserService userService;

    @BeforeEach
    public void beforeEach() {
        realmRepository.save(REALM_DBREPO);
        userRepository.save(USER_1);
        roleRepository.save(ROLE_DEFAULT_RESEARCHER_ROLES);
    }

    @Test
    public void findAll_succeeds() {

        /* test */
        final List<User> response = userService.findAll();
        assertEquals(1, response.size());
    }

    @Test
    public void create_succeeds() throws UserAlreadyExistsException {
        final SignupRequestDto request = SignupRequestDto.builder()
                .username(USER_2_USERNAME)
                .password(USER_2_PASSWORD)
                .email(USER_2_EMAIL)
                .build();

        /* test */
        final User response = userService.create(request, REALM_DBREPO, ROLE_DEFAULT_RESEARCHER_ROLES);
        assertEquals(1, response.getRoles().size());
        final Role role = response.getRoles().get(0);
        assertEquals(ROLE_DEFAULT_RESEARCHER_ROLES_ID, role.getId());
        assertEquals(ROLE_DEFAULT_RESEARCHER_ROLES_NAME, role.getName());
    }

    @Test
    public void create_nonUniqueUsername_fails() {
        final SignupRequestDto request = SignupRequestDto.builder()
                .username(USER_1_USERNAME)
                .password(USER_2_PASSWORD)
                .email(USER_2_EMAIL)
                .build();

        /* test */
        assertThrows(DataIntegrityViolationException.class, () -> {
            userService.create(request, REALM_DBREPO, ROLE_DEFAULT_RESEARCHER_ROLES);
        });
    }

    @Test
    public void create_nonUniqueEmail_fails() {
        final SignupRequestDto request = SignupRequestDto.builder()
                .username(USER_2_USERNAME)
                .password(USER_2_PASSWORD)
                .email(USER_1_EMAIL)
                .build();

        /* test */
        assertThrows(DataIntegrityViolationException.class, () -> {
            userService.create(request, REALM_DBREPO, ROLE_DEFAULT_RESEARCHER_ROLES);
        });
    }

    @Test
    @Transactional
    public void modify_succeeds() throws UserNotFoundException, UserAttributeNotFoundException {
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
        assertEquals(3, response.getAttributes().size());
        final Optional<UserAttribute> affiliation = response.getAttributes().stream().filter(a -> a.getName().equals("affiliation")).findFirst();
        assertTrue(affiliation.isPresent());
        assertEquals("NASA", affiliation.get().getValue());
        final Optional<UserAttribute> orcid = response.getAttributes().stream().filter(a -> a.getName().equals("orcid")).findFirst();
        assertTrue(orcid.isPresent());
        assertNull(orcid.get().getValue());
    }

    @Test
    public void modify_notFound_fails() {
        final UserUpdateDto request = UserUpdateDto.builder()
                .firstname(USER_2_FIRSTNAME)
                .lastname(USER_2_LASTNAME)
                .affiliation(USER_2_AFFILIATION)
                .orcid(USER_2_ORCID)
                .build();

        /* test */
        assertThrows(UserNotFoundException.class, () -> {
            userService.modify(USER_2_ID, request);
        });
    }

    @Test
    public void updatePassword_succeeds() throws UserNotFoundException {
        final UserPasswordDto request = UserPasswordDto.builder()
                .password(USER_1_PASSWORD)
                .build();

        /* test */
        final User response = userService.updatePassword(USER_1_ID, request);
        assertEquals(1, response.getCredentials().size());
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
    public void toggleTheme_succeeds() throws UserNotFoundException, UserAttributeNotFoundException {
        final UserThemeSetDto request = UserThemeSetDto.builder()
                .themeDark(true)
                .build();

        /* test */
        final User response = userService.toggleTheme(USER_1_ID, request);
        assertNotNull(response.getAttributes());
        assertEquals(3, response.getAttributes().size());
    }

    @Test
    public void toggleTheme_fails() {
        final UserThemeSetDto request = UserThemeSetDto.builder()
                .themeDark(true)
                .build();

        /* test */
        assertThrows(UserNotFoundException.class, () -> {
            userService.toggleTheme(USER_2_ID, request);
        });
    }

    @Test
    public void find_succeeds() throws UserNotFoundException {

        /* test */
        final User user = userService.find(USER_1_ID);
        assertEquals(USER_1_ID, user.getId());
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
