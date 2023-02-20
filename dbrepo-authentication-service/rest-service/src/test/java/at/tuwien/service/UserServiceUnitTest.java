package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.auth.SignupRequestDto;
import at.tuwien.api.user.RoleTypeDto;
import at.tuwien.api.user.UserRolesDto;
import at.tuwien.config.AuthenticationConfig;
import at.tuwien.config.ReadyConfig;
import at.tuwien.entities.user.RoleType;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.repositories.UserRepository;
import at.tuwien.service.impl.UserServiceImpl;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class UserServiceUnitTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @MockBean
    private AuthenticationConfig authenticationConfig;

    @MockBean
    private UserRepository userRepository;

    @Autowired
    private UserServiceImpl userService;

    @Test
    public void create_isNotSuperUser_succeeds() throws UserNameExistsException, RoleNotFoundException,
            UserEmailExistsException {

        /* mock */
        when(authenticationConfig.getDefaultRoles())
                .thenReturn(new RoleType[]{RoleType.ROLE_RESEARCHER});
        when(authenticationConfig.getSuperUsers())
                .thenReturn(new String[]{});

        /* test */
        final User response = create_generic(false, false);
        assertEquals(USER_1_USERNAME, response.getUsername());
        assertEquals(USER_1_EMAIL, response.getEmail());
        assertEquals(List.of(RoleType.ROLE_RESEARCHER), response.getRoles());
    }

    @Test
    public void create_emailExists_fails() {

        /* mock */
        when(authenticationConfig.getDefaultRoles())
                .thenReturn(new RoleType[]{RoleType.ROLE_RESEARCHER});
        when(authenticationConfig.getSuperUsers())
                .thenReturn(new String[]{});

        /* test */
        assertThrows(UserEmailExistsException.class, () -> {
            create_generic(false, true);
        });
    }

    @Test
    public void create_usernameExists_fails() {

        /* mock */
        when(authenticationConfig.getDefaultRoles())
                .thenReturn(new RoleType[]{RoleType.ROLE_RESEARCHER});
        when(authenticationConfig.getSuperUsers())
                .thenReturn(new String[]{});

        /* test */
        assertThrows(UserNameExistsException.class, () -> {
            create_generic(true, false);
        });
    }

    @Test
    public void updateRoles_idempotent_succeeds() throws UserNotFoundException, RoleUniqueException,
            RoleNotFoundException {
        final UserRolesDto request = UserRolesDto.builder()
                .roles(List.of(RoleTypeDto.ROLE_RESEARCHER))
                .build();

        /* test */
        final User response = updateRoles_generic(USER_1_ID, USER_1, request);
        assertEquals(USER_1_USERNAME, response.getUsername());
        assertEquals(USER_1_EMAIL, response.getEmail());
        assertEquals(List.of(RoleType.ROLE_RESEARCHER), response.getRoles());
    }

    @Test
    public void updateRoles_addResearcherRole_succeeds() throws UserNotFoundException, RoleUniqueException,
            RoleNotFoundException {
        final UserRolesDto request = UserRolesDto.builder()
                .roles(List.of(RoleTypeDto.ROLE_RESEARCHER))
                .build();

        /* mock */

        /* test */
        final User response = updateRoles_generic(USER_3_ID, USER_3, request);
        assertEquals(USER_3_USERNAME, response.getUsername());
        assertEquals(USER_3_EMAIL, response.getEmail());
        assertEquals(List.of(RoleType.ROLE_RESEARCHER), response.getRoles());
    }

    @Test
    public void updateRoles_addMoreRoles_succeeds() throws UserNotFoundException, RoleUniqueException,
            RoleNotFoundException {
        final UserRolesDto request = UserRolesDto.builder()
                .roles(List.of(RoleTypeDto.ROLE_RESEARCHER, RoleTypeDto.ROLE_DEVELOPER))
                .build();

        /* mock */
        userRepository.save(USER_1);
        userRepository.save(USER_2);
        userRepository.save(USER_3);

        /* test */
        final User response = updateRoles_generic(USER_3_ID, USER_3, request);
        assertEquals(USER_3_USERNAME, response.getUsername());
        assertEquals(USER_3_EMAIL, response.getEmail());
        assertEquals(List.of(RoleType.ROLE_RESEARCHER, RoleType.ROLE_DEVELOPER), response.getRoles());
    }

    @Test
    public void findAll_succeeds() {

        /* mock */
        when(userRepository.findAll())
                .thenReturn(List.of(USER_1, USER_2, USER_3));

        /* test */
        final List<User> response = userService.findAll();
        assertEquals(USER_1, response.get(0));
        assertEquals(USER_2, response.get(1));
        assertEquals(USER_3, response.get(2));
    }

    @Test
    public void find_succeeds() throws UserNotFoundException {

        /* mock */
        when(userRepository.findById(USER_1_ID))
                .thenReturn(Optional.of(USER_1));

        /* test */
        final User response = userService.find(USER_1_ID);
        assertEquals(USER_1, response);
    }

    @Test
    public void find_fails() {

        /* mock */
        when(userRepository.findById(USER_2_ID))
                .thenReturn(Optional.empty());

        /* test */
        assertThrows(UserNotFoundException.class, () -> {
            userService.find(USER_2_ID);
        });
    }

    @Test
    public void validateOrcid_null_succeeds() {

        /* test */
        final boolean response = userService.validateOrcid(null);
        assertTrue(response);
    }

    @Test
    public void validateOrcid_short_fails() {

        /* test */
        final boolean response = userService.validateOrcid("ABC");
        assertFalse(response);
    }

    @Test
    public void validateOrcid_containsX_succeeds() {

        /* test */
        final boolean response = userService.validateOrcid("0000-0003-4216-302X");
        assertTrue(response);
    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    protected User create_generic(boolean usernameExists, boolean emailExists) throws UserNameExistsException,
            RoleNotFoundException, UserEmailExistsException {
        final SignupRequestDto request = SignupRequestDto.builder()
                .username(USER_1_USERNAME)
                .password(USER_1_PASSWORD)
                .email(USER_1_EMAIL)
                .build();

        /* mock */
        if (usernameExists) {
            when(userRepository.findByUsername(USER_1_USERNAME))
                    .thenReturn(Optional.of(USER_1));
        } else {
            when(userRepository.findByUsername(USER_1_USERNAME))
                    .thenReturn(Optional.empty());
        }
        if (emailExists) {
            when(userRepository.findByEmail(USER_1_EMAIL))
                    .thenReturn(Optional.of(USER_1));
        } else {
            when(userRepository.findByEmail(USER_1_EMAIL))
                    .thenReturn(Optional.empty());
        }
        when(userRepository.save(any(User.class)))
                .thenReturn(USER_1);

        /* test */
        return userService.create(request);
    }

    protected User updateRoles_generic(Long userId, User user, UserRolesDto data) throws RoleNotFoundException,
            UserNotFoundException, RoleUniqueException {

        /* mock */
        if (user != null) {
            when(userRepository.findById(userId))
                    .thenReturn(Optional.of(user));
        } else {
            when(userRepository.findById(userId))
                    .thenReturn(Optional.empty());
        }
        when(userRepository.save(any(User.class)))
                .thenReturn(user);

        /* test */
        return userService.updateRoles(userId, data);
    }

}
