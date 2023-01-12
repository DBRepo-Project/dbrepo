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
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@Log4j2
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class UserServiceUnitTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @MockBean
    private AuthenticationConfig authenticationConfig;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Test
    public void create_isDeveloper_succeeds() throws UserNameExistsException, RoleNotFoundException,
            UserEmailExistsException {
        final SignupRequestDto request = SignupRequestDto.builder()
                .username(USER_1_USERNAME)
                .password(USER_1_PASSWORD)
                .email(USER_1_EMAIL)
                .build();

        /* mock */
        when(authenticationConfig.getDefaultRoles())
                .thenReturn(new RoleType[]{RoleType.ROLE_RESEARCHER});
        when(authenticationConfig.getDeveloperUsernames())
                .thenReturn(new String[]{USER_1_USERNAME});

        /* test */
        final User response = userService.create(request);
        assertEquals(USER_1_USERNAME, response.getUsername());
        assertEquals(USER_1_EMAIL, response.getEmail());
        assertEquals(List.of(RoleType.ROLE_RESEARCHER, RoleType.ROLE_DEVELOPER), response.getRoles());
    }

    @Test
    public void create_isNotDeveloper_succeeds() throws UserNameExistsException, RoleNotFoundException,
            UserEmailExistsException {
        final SignupRequestDto request = SignupRequestDto.builder()
                .username(USER_1_USERNAME)
                .password(USER_1_PASSWORD)
                .email(USER_1_EMAIL)
                .build();

        /* mock */
        when(authenticationConfig.getDefaultRoles())
                .thenReturn(new RoleType[]{RoleType.ROLE_RESEARCHER});
        when(authenticationConfig.getDeveloperUsernames())
                .thenReturn(new String[]{});

        /* test */
        final User response = userService.create(request);
        assertEquals(USER_1_USERNAME, response.getUsername());
        assertEquals(USER_1_EMAIL, response.getEmail());
        assertEquals(List.of(RoleType.ROLE_RESEARCHER), response.getRoles());
    }

    @Test
    public void create_noRole_succeeds() throws UserNameExistsException, RoleNotFoundException,
            UserEmailExistsException {
        final SignupRequestDto request = SignupRequestDto.builder()
                .username(USER_1_USERNAME)
                .password(USER_1_PASSWORD)
                .email(USER_1_EMAIL)
                .build();

        /* mock */
        when(authenticationConfig.getDefaultRoles())
                .thenReturn(new RoleType[]{});
        when(authenticationConfig.getDeveloperUsernames())
                .thenReturn(new String[]{});

        /* test */
        final User response = userService.create(request);
        assertEquals(USER_1_USERNAME, response.getUsername());
        assertEquals(USER_1_EMAIL, response.getEmail());
        assertEquals(List.of(), response.getRoles());
    }

    @Test
    public void updateRoles_idempotent_succeeds() throws UserNotFoundException, RoleUniqueException,
            RoleNotFoundException {
        final UserRolesDto request = UserRolesDto.builder()
                .roles(List.of(RoleTypeDto.ROLE_RESEARCHER))
                .build();

        /* mock */
        userRepository.save(USER_1);

        /* test */
        final User response = userService.updateRoles(USER_1_ID, request);
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
        userRepository.save(USER_1);
        userRepository.save(USER_2);
        userRepository.save(USER_3);

        /* test */
        final User response = userService.updateRoles(USER_3_ID, request);
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
        final User response = userService.updateRoles(USER_3_ID, request);
        assertEquals(USER_3_USERNAME, response.getUsername());
        assertEquals(USER_3_EMAIL, response.getEmail());
        assertEquals(List.of(RoleType.ROLE_RESEARCHER, RoleType.ROLE_DEVELOPER), response.getRoles());
    }

}
