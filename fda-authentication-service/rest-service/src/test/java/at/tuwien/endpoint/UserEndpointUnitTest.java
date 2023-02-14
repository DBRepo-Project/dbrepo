package at.tuwien.endpoint;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.amqp.CreateUserDto;
import at.tuwien.api.amqp.UserDetailsDto;
import at.tuwien.api.auth.SignupRequestDto;
import at.tuwien.api.user.*;
import at.tuwien.endpoints.UserEndpoint;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.repositories.UserRepository;
import at.tuwien.service.MailService;
import at.tuwien.service.QueueService;
import at.tuwien.service.TimeSecretService;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.thymeleaf.context.Context;

import javax.servlet.http.HttpServletResponse;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class UserEndpointUnitTest extends BaseUnitTest {

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private TimeSecretService timeSecretService;

    @MockBean
    private QueueService queueService;

    @MockBean
    private MailService mailService;

    @Bean
    @Primary
    public UserDetailsService userDetailsService() {
        return new InMemoryUserDetailsManager(List.of(USER_1_DETAILS, USER_2_DETAILS, USER_3_DETAILS));
    }

    @Autowired
    private UserEndpoint userEndpoint;

    @Test
    public void updateRoles_anonymous_fails() {
        final UserRolesDto request = UserRolesDto.builder()
                .roles(List.of(RoleTypeDto.ROLE_RESEARCHER))
                .build();

        /* test */
        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> {
            updateRoles_generic(USER_3_ID, USER_3, request);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void updateRoles_researcherResearcher_fails() {
        final UserRolesDto request = UserRolesDto.builder()
                .roles(List.of(RoleTypeDto.ROLE_RESEARCHER))
                .build();

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            updateRoles_generic(USER_3_ID, USER_3, request);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void updateRoles_researcherDeveloper_succeeds() throws UserNotFoundException, RoleUniqueException,
            OrcidMalformedException, RoleNotFoundException {
        final UserRolesDto request = UserRolesDto.builder()
                .roles(List.of(RoleTypeDto.ROLE_RESEARCHER))
                .build();

        /* test */
        updateRoles_generic(USER_3_ID, USER_3, request);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"RESEARCHER", "DEVELOPER"})
    public void updateRoles_researcherDeveloperAndRearcher_succeeds() throws UserNotFoundException, RoleUniqueException,
            OrcidMalformedException, RoleNotFoundException {
        final UserRolesDto request = UserRolesDto.builder()
                .roles(List.of(RoleTypeDto.ROLE_RESEARCHER))
                .build();

        /* test */
        updateRoles_generic(USER_3_ID, USER_3, request);
    }

    @Test
    public void updateTheme_anonymous_fails() {

        /* test */
        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> {
            updateTheme_generic(USER_1_ID, USER_1, USER_THEME_DARK_DTO);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void updateTheme_researcherDark_succeeds() throws UserNotFoundException {

        /* test */
        updateTheme_generic(USER_1_ID, USER_1, USER_THEME_DARK_DTO);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void updateTheme_researcherLightSame_succeeds() throws UserNotFoundException {

        /* test */
        updateTheme_generic(USER_1_ID, USER_1, USER_THEME_LIGHT_DTO);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void updateTheme_researcherDarkNotFound_fails() {

        /* test */
        assertThrows(UsernameNotFoundException.class, () -> {
            updateTheme_generic(USER_1_ID, null, USER_THEME_DARK_DTO);
        });
    }

    @Test
    public void find_anonymous_fails() {

        /* test */
        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> {
            find_generic(USER_2_ID);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void find_researcher_fails() {

        /* mock */
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));
        when(userRepository.findById(USER_2_ID))
                .thenReturn(Optional.of(USER_2));

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            find_generic(USER_2_ID);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void find_developer_succeeds() throws UserNotFoundException, OrcidMalformedException {

        /* mock */
        when(userRepository.findByUsername(USER_2_USERNAME))
                .thenReturn(Optional.of(USER_2));
        when(userRepository.findById(USER_1_ID))
                .thenReturn(Optional.of(USER_1));

        /* test */
        find_generic(USER_1_ID);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void find_researcherNotFound_succeeds() {

        /* mock */
        when(userRepository.findByUsername(USER_2_USERNAME))
                .thenReturn(Optional.of(USER_2));
        when(userRepository.findById(USER_1_ID))
                .thenReturn(Optional.empty());

        /* test */
        assertThrows(UserNotFoundException.class, () -> {
            find_generic(USER_1_ID);
        });
    }

    @Test
    public void list_anonymous_succeeds() {

        /* test */
        list_generic();
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void list_researcher_succeeds() {

        /* mock */
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));

        /* test */
        list_generic();
    }

    @Test
    public void update_anonymous_fails() {
        final UserUpdateDto request = UserUpdateDto.builder()
                .firstname(USER_2_FIRSTNAME)
                .lastname(USER_2_LASTNAME)
                .affiliation(USER_2_AFFILIATION)
                .orcid(USER_2_ORCID_UNCOMPRESSED)
                .build();

        /* test */
        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> {
            update_generic(USER_2_ID, USER_2, request);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void update_developerModifyOther_succeeds() throws UserNotFoundException, OrcidMalformedException {
        final UserUpdateDto request = UserUpdateDto.builder()
                .firstname(USER_1_FIRSTNAME)
                .lastname(USER_1_LASTNAME)
                .affiliation(USER_1_AFFILIATION)
                .orcid(USER_1_ORCID_UNCOMPRESSED)
                .build();

        /* mock */
        when(userRepository.findByUsername(USER_2_USERNAME))
                .thenReturn(Optional.of(USER_2));
        when(userRepository.findById(USER_1_ID))
                .thenReturn(Optional.of(USER_1));

        /* test */
        final UserDto response = update_generic(USER_1_ID, USER_1, request);
        assertEquals(USER_1_ID, response.getId());
        assertEquals(USER_1_USERNAME, response.getUsername());
        assertEquals(USER_1_FIRSTNAME, response.getFirstname());
        assertEquals(USER_1_LASTNAME, response.getLastname());
        assertEquals(USER_1_ORCID_UNCOMPRESSED, response.getOrcid());
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void update_developerModifyOtherNotFound_fails() {
        final UserUpdateDto request = UserUpdateDto.builder()
                .firstname(USER_1_FIRSTNAME)
                .lastname(USER_1_LASTNAME)
                .affiliation(USER_1_AFFILIATION)
                .orcid(USER_1_ORCID_UNCOMPRESSED)
                .build();

        /* mock */
        when(userRepository.findByUsername(USER_2_USERNAME))
                .thenReturn(Optional.of(USER_2));
        when(userRepository.findById(USER_1_ID))
                .thenReturn(Optional.empty());

        /* test */
        assertThrows(UserNotFoundException.class, () -> {
            update_generic(USER_1_ID, USER_1, request);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void update_developer_succeeds() throws UserNotFoundException, OrcidMalformedException {
        final UserUpdateDto request = UserUpdateDto.builder()
                .firstname(USER_1_FIRSTNAME)
                .lastname(USER_1_LASTNAME)
                .affiliation(USER_1_AFFILIATION)
                .orcid(USER_1_ORCID_UNCOMPRESSED)
                .build();

        /* mock */
        when(userRepository.findByUsername(USER_2_USERNAME))
                .thenReturn(Optional.of(USER_2));
        when(userRepository.findById(USER_1_ID))
                .thenReturn(Optional.of(USER_1));

        /* test */
        final UserDto response = update_generic(USER_1_ID, USER_1, request);
        assertEquals(USER_1_ID, response.getId());
        assertEquals(USER_1_USERNAME, response.getUsername());
        assertEquals(USER_1_FIRSTNAME, response.getFirstname());
        assertEquals(USER_1_LASTNAME, response.getLastname());
        assertEquals(USER_1_ORCID_UNCOMPRESSED, response.getOrcid());
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void update_researcher_succeeds() throws UserNotFoundException, OrcidMalformedException {
        final UserUpdateDto request = UserUpdateDto.builder()
                .firstname(USER_1_FIRSTNAME)
                .lastname(USER_1_LASTNAME)
                .affiliation(USER_1_AFFILIATION)
                .orcid(USER_1_ORCID_UNCOMPRESSED)
                .build();

        /* mock */
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));
        when(userRepository.findById(USER_1_ID))
                .thenReturn(Optional.of(USER_1));

        /* test */
        final UserDto response = update_generic(USER_1_ID, USER_1, request);
        assertEquals(USER_1_ID, response.getId());
        assertEquals(USER_1_USERNAME, response.getUsername());
        assertEquals(USER_1_FIRSTNAME, response.getFirstname());
        assertEquals(USER_1_LASTNAME, response.getLastname());
        assertEquals(USER_1_ORCID_UNCOMPRESSED, response.getOrcid());
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void update_researcherModifyOther_fails() {
        final UserUpdateDto request = UserUpdateDto.builder()
                .firstname(USER_1_FIRSTNAME)
                .lastname(USER_1_LASTNAME)
                .affiliation(USER_1_AFFILIATION)
                .orcid(USER_1_ORCID_UNCOMPRESSED)
                .build();

        /* mock */
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));
        when(userRepository.findById(USER_2_ID))
                .thenReturn(Optional.of(USER_2));

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            update_generic(USER_2_ID, USER_2, request);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void update_researcherInvalidOrcid_fails() {
        final UserUpdateDto request = UserUpdateDto.builder()
                .firstname(USER_1_FIRSTNAME)
                .lastname(USER_1_LASTNAME)
                .affiliation(USER_1_AFFILIATION)
                .orcid("0000-0003-4216-3020")
                .build();

        /* mock */
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));
        when(userRepository.findById(USER_1_ID))
                .thenReturn(Optional.of(USER_1));

        /* test */
        assertThrows(OrcidMalformedException.class, () -> {
            update_generic(USER_1_ID, USER_1, request);
        });
    }

    @Test
    public void register_anonymous_succeeds() throws UserNameExistsException, UserEmailFailedException,
            BrokerUserCreationException, OrcidMalformedException, RoleNotFoundException, UserEmailExistsException,
            NotAllowedException {

        /* test */
        final UserDto response = register_generic(USER_1_USERNAME, USER_1, null, USER_1_SIGNUP_REQUEST_DTO);
        assertEquals(USER_1_ID, response.getId());
        assertEquals(USER_1_USERNAME, response.getUsername());
        assertEquals(USER_1_EMAIL, response.getEmail());
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void register_researcher_fails() {

        /* mock */
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            register_generic(USER_2_USERNAME, USER_2, USER_1_PRINCIPAL, USER_2_SIGNUP_REQUEST_DTO);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void register_developer_fails() {

        /* mock */
        when(userRepository.findByUsername(USER_2_USERNAME))
                .thenReturn(Optional.of(USER_2));

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            register_generic(USER_1_USERNAME, USER_1, USER_2_PRINCIPAL, USER_1_SIGNUP_REQUEST_DTO);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void register_dataSteward_fails() {

        /* mock */
        when(userRepository.findByUsername(USER_3_USERNAME))
                .thenReturn(Optional.of(USER_3));

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            register_generic(USER_1_USERNAME, USER_1, USER_3_PRINCIPAL, USER_1_SIGNUP_REQUEST_DTO);
        });
    }

    @Test
    public void updateEmail_anonymous_fails() {
        final UserEmailDto request = UserEmailDto.builder()
                .email(USER_1_EMAIL)
                .build();

        /* test */
        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> {
            updateEmail_generic(USER_1_ID, null, request);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void updateEmail_researcher_succeeds() throws UserNotFoundException, UserEmailFailedException,
            OrcidMalformedException {
        final UserEmailDto request = UserEmailDto.builder()
                .email(USER_1_EMAIL)
                .build();

        /* mock */
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));

        /* test */
        updateEmail_generic(USER_1_ID, USER_1, request);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void updateEmail_developer_succeeds() throws UserNotFoundException, UserEmailFailedException,
            OrcidMalformedException {
        final UserEmailDto request = UserEmailDto.builder()
                .email(USER_2_EMAIL)
                .build();

        /* mock */
        when(userRepository.findByUsername(USER_2_USERNAME))
                .thenReturn(Optional.of(USER_2));

        /* test */
        updateEmail_generic(USER_2_ID, USER_2, request);
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void updateEmail_dataSteward_succeeds() throws UserNotFoundException, UserEmailFailedException,
            OrcidMalformedException {
        final UserEmailDto request = UserEmailDto.builder()
                .email(USER_3_EMAIL)
                .build();

        /* mock */
        when(userRepository.findByUsername(USER_3_USERNAME))
                .thenReturn(Optional.of(USER_3));

        /* test */
        updateEmail_generic(USER_3_ID, USER_3, request);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void updateEmail_differentUser_fails() {
        final UserEmailDto request = UserEmailDto.builder()
                .email(USER_1_EMAIL)
                .build();

        /* mock */
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            updateEmail_generic(USER_2_ID, USER_2, request);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void updateEmail_notExists_fails() {
        final UserEmailDto request = UserEmailDto.builder()
                .email(USER_1_EMAIL)
                .build();

        /* mock */
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            updateEmail_generic(USER_1_ID, null, request);
        });
    }

    @Test
    public void updatePassword_anonymous_fails() {
        final UserPasswordDto request = UserPasswordDto.builder()
                .password(USER_1_PASSWORD)
                .build();

        /* test */
        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> {
            updatePassword_generic(USER_1_ID, USER_1_USERNAME, null, USER_1_DETAILS_DTO, request);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void updatePassword_researcher_succeeds() throws UserNotFoundException, UserEmailFailedException,
            OrcidMalformedException, BrokerUserCreationException {
        final UserPasswordDto request = UserPasswordDto.builder()
                .password(USER_1_PASSWORD)
                .build();

        /* mock */
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));

        /* test */
        updatePassword_generic(USER_1_ID, USER_1_USERNAME, USER_1, USER_1_DETAILS_DTO, request);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void updatePassword_developer_succeeds() throws UserNotFoundException, UserEmailFailedException,
            OrcidMalformedException, BrokerUserCreationException {
        final UserPasswordDto request = UserPasswordDto.builder()
                .password(USER_2_PASSWORD)
                .build();

        /* mock */
        when(userRepository.findByUsername(USER_2_USERNAME))
                .thenReturn(Optional.of(USER_2));

        /* test */
        updatePassword_generic(USER_2_ID, USER_2_USERNAME, USER_2, USER_2_DETAILS_DTO, request);
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void updatePassword_dataSteward_succeeds() throws UserNotFoundException, UserEmailFailedException,
            OrcidMalformedException, BrokerUserCreationException {
        final UserPasswordDto request = UserPasswordDto.builder()
                .password(USER_3_PASSWORD)
                .build();

        /* mock */
        when(userRepository.findByUsername(USER_3_USERNAME))
                .thenReturn(Optional.of(USER_3));

        /* test */
        updatePassword_generic(USER_3_ID, USER_3_USERNAME, USER_3, USER_3_DETAILS_DTO, request);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void updatePassword_researcherRabbitMqRoles_succeeds() throws UserNotFoundException, UserEmailFailedException,
            OrcidMalformedException, BrokerUserCreationException {
        final UserPasswordDto request = UserPasswordDto.builder()
                .password(USER_1_PASSWORD)
                .build();

        /* mock */
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));

        /* test */
        updatePassword_generic(USER_1_ID, USER_1_USERNAME, USER_1, USER_1_DETAILS_WITH_TAGS_DTO, request);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void updatePassword_differentUser_fails() {
        final UserPasswordDto request = UserPasswordDto.builder()
                .password(USER_1_PASSWORD)
                .build();

        /* mock */
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            updatePassword_generic(USER_2_ID, USER_2_USERNAME, USER_2, USER_2_DETAILS_DTO, request);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void updatePassword_notExists_fails() {
        final UserPasswordDto request = UserPasswordDto.builder()
                .password(USER_1_PASSWORD)
                .build();

        /* mock */
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            updatePassword_generic(USER_1_ID, USER_1_USERNAME, null, USER_1_DETAILS_DTO, request);
        });
    }

    @Test
    public void forgot_anonymous_succeeds() throws UserNotFoundException, NotAllowedException, UserEmailFailedException,
            OrcidMalformedException {
        final UserForgotDto request = UserForgotDto.builder()
                .username(USER_1_USERNAME)
                .email(USER_1_EMAIL)
                .build();

        /* mock */
        when(userRepository.findByUsernameOrEmail(USER_1_USERNAME, USER_1_EMAIL))
                .thenReturn(Optional.of(USER_1));

        /* test */
        forgot_generic(USER_1, null, request);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void forgot_researcher_fails() {
        final UserForgotDto request = UserForgotDto.builder()
                .username(USER_1_USERNAME)
                .email(USER_1_EMAIL)
                .build();

        /* mock */
        when(userRepository.findByUsernameOrEmail(USER_1_USERNAME, USER_1_EMAIL))
                .thenReturn(Optional.of(USER_1));

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            forgot_generic(USER_1, USER_1_PRINCIPAL, request);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void forgot_developer_fails() {
        final UserForgotDto request = UserForgotDto.builder()
                .username(USER_2_USERNAME)
                .email(USER_2_EMAIL)
                .build();

        /* mock */
        when(userRepository.findByUsernameOrEmail(USER_2_USERNAME, USER_2_EMAIL))
                .thenReturn(Optional.of(USER_2));

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            forgot_generic(USER_2, USER_2_PRINCIPAL, request);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void forgot_dataSteward_fails() {
        final UserForgotDto request = UserForgotDto.builder()
                .username(USER_3_USERNAME)
                .email(USER_3_EMAIL)
                .build();

        /* mock */
        when(userRepository.findByUsernameOrEmail(USER_3_USERNAME, USER_3_EMAIL))
                .thenReturn(Optional.of(USER_3));

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            forgot_generic(USER_3, USER_3_PRINCIPAL, request);
        });
    }

    @Test
    public void reset_anonymous_succeeds() throws UserNotFoundException, NotAllowedException, UserEmailFailedException,
            BrokerUserCreationException, SecretInvalidException {
        final UserResetDto request = UserResetDto.builder()
                .password(USER_1_PASSWORD)
                .token(TOKEN_1_TOKEN)
                .build();

        /* mock */
        when(userRepository.findByUsernameOrEmail(USER_1_USERNAME, USER_1_EMAIL))
                .thenReturn(Optional.of(USER_1));

        /* test */
        reset_generic(USER_1, null, request);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void reset_researcher_fails() {
        final UserResetDto request = UserResetDto.builder()
                .password(USER_1_PASSWORD)
                .token(TOKEN_1_TOKEN)
                .build();

        /* mock */
        when(userRepository.findByUsernameOrEmail(USER_1_USERNAME, USER_1_EMAIL))
                .thenReturn(Optional.of(USER_1));

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            reset_generic(USER_1, USER_1_PRINCIPAL, request);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void reset_developer_fails() {
        final UserResetDto request = UserResetDto.builder()
                .password(USER_2_PASSWORD)
                .token(TOKEN_2_TOKEN)
                .build();

        /* mock */
        when(userRepository.findByUsernameOrEmail(USER_2_USERNAME, USER_2_EMAIL))
                .thenReturn(Optional.of(USER_2));

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            reset_generic(USER_2, USER_2_PRINCIPAL, request);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void reset_dataSteward_fails() {
        final UserResetDto request = UserResetDto.builder()
                .password(USER_3_PASSWORD)
                .token(TOKEN_3_TOKEN)
                .build();

        /* mock */
        when(userRepository.findByUsernameOrEmail(USER_3_USERNAME, USER_3_EMAIL))
                .thenReturn(Optional.of(USER_3));

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            reset_generic(USER_3, USER_3_PRINCIPAL, request);
        });
    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    protected void updateRoles_generic(Long userId, User user, UserRolesDto data) throws UserNotFoundException,
            RoleUniqueException, OrcidMalformedException, RoleNotFoundException {

        /* mock */
        if (user == null) {
            when(userRepository.findById(userId))
                    .thenReturn(Optional.empty());
        } else {
            when(userRepository.findById(userId))
                    .thenReturn(Optional.of(user));
        }
        when(userRepository.save(any(User.class)))
                .thenReturn(user);

        /* test */
        final ResponseEntity<UserDto> response = userEndpoint.updateRoles(userId, data);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    protected void updateTheme_generic(Long userId, User user, UserThemeSetDto data) throws UserNotFoundException {

        /* mock */
        if (user == null) {
            when(userRepository.findByUsername(anyString()))
                    .thenReturn(Optional.empty());
            when(userRepository.findById(userId))
                    .thenReturn(Optional.empty());
        } else {
            when(userRepository.findByUsername(user.getUsername()))
                    .thenReturn(Optional.of(user));
            when(userRepository.findById(userId))
                    .thenReturn(Optional.of(user));
        }
        when(userRepository.save(any(User.class)))
                .thenReturn(user);

        /* test */
        final ResponseEntity<Void> response = userEndpoint.updateTheme(userId, data);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNull(response.getBody());
    }

    protected void find_generic(Long userId) throws UserNotFoundException, OrcidMalformedException {

        /* test */
        final ResponseEntity<UserDto> response = userEndpoint.find(userId);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        final UserDto body = response.getBody();
        assertEquals(USER_1_ID, body.getId());
        assertEquals(USER_1_USERNAME, body.getUsername());
        assertEquals(USER_1_EMAIL, body.getEmail());
    }

    protected void list_generic() {

        /* mock */
        when(userRepository.findAll())
                .thenReturn(List.of(USER_2));

        /* test */
        final ResponseEntity<List<UserBriefDto>> response = userEndpoint.list();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        final List<UserBriefDto> body = response.getBody();
        assertEquals(1, body.size());
        assertEquals(USER_2_ID, body.get(0).getId());
        assertEquals(USER_2_USERNAME, body.get(0).getUsername());
        assertEquals(List.of("ROLE_DEVELOPER"), body.get(0).getRoles());
    }

    protected UserDto update_generic(Long userId, User user, UserUpdateDto data) throws UserNotFoundException,
            OrcidMalformedException {

        /* mock */
        when(userRepository.save(any(User.class)))
                .thenReturn(user);

        /* test */
        final ResponseEntity<UserDto> response = userEndpoint.update(userId, data);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
        return response.getBody();
    }

    protected UserDto register_generic(String username, User user, Principal principal, SignupRequestDto data)
            throws OrcidMalformedException, UserNameExistsException, UserEmailFailedException,
            BrokerUserCreationException, RoleNotFoundException, UserEmailExistsException, NotAllowedException {

        /* mock */
        when(userRepository.save(any(User.class)))
                .thenReturn(user);
        doNothing()
                .when(queueService)
                .createUser(eq(username), eq(data));
        when(timeSecretService.create(any(User.class)))
                .thenReturn(TIME_SECRET_1);
        doNothing()
                .when(mailService)
                .send(eq(user), anyString(), anyString(), any(Context.class));

        /* test */
        final ResponseEntity<UserDto> response = userEndpoint.register(data, principal);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        return response.getBody();
    }

    protected void updateEmail_generic(Long userId, User user, UserEmailDto data)
            throws OrcidMalformedException, UserEmailFailedException, UserNotFoundException {

        /* mock */
        if (user == null) {
            when(userRepository.findById(userId))
                    .thenReturn(Optional.empty());
        } else {
            when(userRepository.findById(userId))
                    .thenReturn(Optional.of(user));
        }
        when(userRepository.save(any(User.class)))
                .thenReturn(user);
        when(timeSecretService.create(any(User.class)))
                .thenReturn(TIME_SECRET_1);
        doNothing()
                .when(mailService)
                .send(eq(user), anyString(), anyString(), any(Context.class));

        /* test */
        final ResponseEntity<UserDto> response = userEndpoint.updateEmail(userId, data);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    protected void updatePassword_generic(Long userId, String username, User user, UserDetailsDto userDetails,
                                          UserPasswordDto data) throws OrcidMalformedException,
            UserEmailFailedException, UserNotFoundException, BrokerUserCreationException {

        /* mock */
        if (user == null) {
            when(userRepository.findById(userId))
                    .thenReturn(Optional.empty());
        } else {
            when(userRepository.findById(userId))
                    .thenReturn(Optional.of(user));
        }
        when(queueService.findUser(username))
                .thenReturn(userDetails);
        doNothing()
                .when(queueService)
                .modifyUserPassword(eq(user), any(CreateUserDto.class));
        when(userRepository.save(any(User.class)))
                .thenReturn(user);
        doNothing()
                .when(mailService)
                .send(eq(user), anyString(), anyString(), any(Context.class));

        /* test */
        final ResponseEntity<UserDto> response = userEndpoint.updatePassword(userId, data);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    protected void forgot_generic(User user, Principal principal, UserForgotDto data) throws UserNotFoundException,
            NotAllowedException, UserEmailFailedException, OrcidMalformedException {

        /* mock */
        when(timeSecretService.create(any(User.class)))
                .thenReturn(TIME_SECRET_1);
        when(userRepository.save(any(User.class)))
                .thenReturn(user);
        doNothing()
                .when(mailService)
                .send(eq(user), anyString(), anyString(), any(Context.class));

        /* test */
        final ResponseEntity<UserDto> response = userEndpoint.forgot(data, principal);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    protected void reset_generic(User user, Principal principal, UserResetDto data) throws UserNotFoundException,
            NotAllowedException, UserEmailFailedException, BrokerUserCreationException, SecretInvalidException {
        final HttpServletResponse mock = new MockHttpServletResponse();

        /* mock */
        when(timeSecretService.invalidate(data.getToken()))
                .thenReturn(user);
        when(userRepository.save(any(User.class)))
                .thenReturn(user);
        when( userRepository.findById(user.getId()))
                .thenReturn(Optional.of(user));
        doNothing()
                .when(mailService)
                .send(eq(user), anyString(), anyString(), any(Context.class));

        /* test */
        userEndpoint.reset(data, mock, principal);
        final String header = mock.getHeader("Location");
        assertNotNull(header);
        assertTrue(header.contains("/login"));
        assertEquals(302, mock.getStatus());
    }


}
