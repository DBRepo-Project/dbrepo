package at.tuwien.endpoint;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.user.*;
import at.tuwien.endpoints.UserEndpoint;
import at.tuwien.entities.user.RoleType;
import at.tuwien.entities.user.User;
import at.tuwien.exception.OrcidMalformedException;
import at.tuwien.exception.RoleNotFoundException;
import at.tuwien.exception.RoleUniqueException;
import at.tuwien.exception.UserNotFoundException;
import at.tuwien.repositories.UserRepository;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@Log4j2
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@ExtendWith(SpringExtension.class)
public class UserEndpointUnitTest extends BaseUnitTest {

    @MockBean
    private UserRepository userRepository;

    @Bean
    @Primary
    public UserDetailsService userDetailsService() {
        return new InMemoryUserDetailsManager(List.of(USER_1_DETAILS, USER_2_DETAILS, USER_3_DETAILS));
    }

    @Autowired
    private UserEndpoint userEndpoint;

    @Test
    @WithAnonymousUser
    public void updateRoles_anonymous_fails() {
        final UserRolesDto request = UserRolesDto.builder()
                .roles(List.of(RoleTypeDto.ROLE_RESEARCHER))
                .build();

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
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
                .orcid(USER_2_ORCID)
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
                .orcid(USER_1_ORCID)
                .build();

        /* mock */
        when(userRepository.findByUsername(USER_2_USERNAME))
                .thenReturn(Optional.of(USER_2));
        when(userRepository.findById(USER_1_ID))
                .thenReturn(Optional.of(USER_1));

        /* test */
        update_generic(USER_1_ID, USER_1, request);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void update_developerModifyOtherNotFound_fails() {
        final UserUpdateDto request = UserUpdateDto.builder()
                .firstname(USER_1_FIRSTNAME)
                .lastname(USER_1_LASTNAME)
                .affiliation(USER_1_AFFILIATION)
                .orcid(USER_1_ORCID)
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
                .orcid(USER_1_ORCID)
                .build();

        /* mock */
        when(userRepository.findByUsername(USER_2_USERNAME))
                .thenReturn(Optional.of(USER_2));
        when(userRepository.findById(USER_2_ID))
                .thenReturn(Optional.of(USER_2));

        /* test */
        update_generic(USER_2_ID, USER_2, request);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void update_researcher_succeeds() throws UserNotFoundException, OrcidMalformedException {
        final UserUpdateDto request = UserUpdateDto.builder()
                .firstname(USER_1_FIRSTNAME)
                .lastname(USER_1_LASTNAME)
                .affiliation(USER_1_AFFILIATION)
                .orcid(USER_1_ORCID)
                .build();

        /* mock */
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));
        when(userRepository.findById(USER_1_ID))
                .thenReturn(Optional.of(USER_1));

        /* test */
        update_generic(USER_1_ID, USER_1, request);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void update_researcherModifyOther_fails() {
        final UserUpdateDto request = UserUpdateDto.builder()
                .firstname(USER_1_FIRSTNAME)
                .lastname(USER_1_LASTNAME)
                .affiliation(USER_1_AFFILIATION)
                .orcid(USER_1_ORCID)
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


}
