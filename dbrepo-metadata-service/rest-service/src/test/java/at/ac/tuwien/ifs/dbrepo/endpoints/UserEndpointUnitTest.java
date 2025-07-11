package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.api.auth.CreateUserDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.UserBriefDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.UserDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.UserUpdateDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.user.User;
import at.ac.tuwien.ifs.dbrepo.core.exception.AuthServiceException;
import at.ac.tuwien.ifs.dbrepo.core.exception.NotAllowedException;
import at.ac.tuwien.ifs.dbrepo.core.exception.UserNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.security.Principal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@Slf4j
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class UserEndpointUnitTest extends BaseTest {

    @MockBean
    private UserService userService;

    @Autowired
    private UserEndpoint userEndpoint;

    @Test
    @WithAnonymousUser
    public void findAll_anonymous_succeeds() throws UserNotFoundException {

        /* test */
        final List<UserBriefDto> response = findAll_generic(null, null);
        assertEquals(2, response.size());
    }

    @Test
    @WithAnonymousUser
    public void findAll_filterInternalUserEmptyList_succeeds() throws UserNotFoundException {

        /* test */
        final List<UserBriefDto> response = findAll_generic(USER_LOCAL_ADMIN_USERNAME, null);
        assertEquals(0, response.size());
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void findAll_noRole_succeeds() throws UserNotFoundException {

        /* test */
        final List<UserBriefDto> response = findAll_generic(null, null);
        assertEquals(2, response.size());
    }

    @Test
    public void findAll_filterUsername_succeeds() throws UserNotFoundException {

        /* test */
        final List<UserBriefDto> response = findAll_generic(USER_2_USERNAME, USER_2);
        assertEquals(1, response.size());
        assertEquals(USER_2_USERNAME, response.get(0).getUsername());
    }

    @Test
    public void findAll_filterUsername_fails() throws UserNotFoundException {

        /* test */
        final List<UserBriefDto> response = findAll_generic(USER_5_USERNAME, null);
        assertEquals(0, response.size());
    }

    @Test
    @WithAnonymousUser
    public void find_anonymous_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            find_generic(null, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void find_self_succeeds() throws NotAllowedException, UserNotFoundException {

        /* test */
        find_generic(USER_1_USERNAME, USER_1, USER_1_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void find_foreign_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            find_generic(USER_2_USERNAME, USER_2, USER_1_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, authorities = {"find-foreign-user"})
    public void find_hasRoleForeign_succeeds() throws UserNotFoundException, NotAllowedException {
        final Principal principal = new UsernamePasswordAuthenticationToken(USER_3_DETAILS, USER_3_PASSWORD, List.of(
                new SimpleGrantedAuthority("find-foreign-user")));

        /* test */
        find_generic(USER_2_USERNAME, USER_2, principal);
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, authorities = {"system"})
    public void find_system_succeeds() throws UserNotFoundException, NotAllowedException {
        final Principal principal = new UsernamePasswordAuthenticationToken(USER_3_DETAILS, USER_3_PASSWORD, List.of(
                new SimpleGrantedAuthority("system")));

        /* test */
        final ResponseEntity<UserDto> response = find_generic(USER_3_USERNAME, USER_3, principal);
        assertNotNull(response.getHeaders().get("X-Username"));
        assertEquals(USER_3.getUsername(), response.getHeaders().get("X-Username").get(0));
        assertNotNull(response.getHeaders().get("X-Password"));
        assertNotEquals(USER_3_PASSWORD, response.getHeaders().get("X-Password").get(0));
        assertEquals(USER_3_DATABASE_PASSWORD, response.getHeaders().get("X-Password").get(0));
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void find_internalUser_fails() {
        final Principal principal = new UsernamePasswordAuthenticationToken(USER_LOCAL_ADMIN_DETAILS, USER_LOCAL_ADMIN_PASSWORD, List.of(
                new SimpleGrantedAuthority("system")));

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            find_generic(USER_LOCAL_ADMIN_USERNAME, USER_LOCAL, principal);
        });
    }

    @Test
    @WithAnonymousUser
    public void modify_anonymous_fails() {
        final UserUpdateDto request = UserUpdateDto.builder()
                .firstname(USER_1_FIRSTNAME)
                .lastname(USER_1_LASTNAME)
                .affiliation(USER_1_AFFILIATION)
                .orcid(USER_1_ORCID_URL)
                .build();

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            modify_generic(null, null, null, request);
        });
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void modify_noRole_fails() {
        final UserUpdateDto request = UserUpdateDto.builder()
                .firstname(USER_1_FIRSTNAME)
                .lastname(USER_1_LASTNAME)
                .affiliation(USER_1_AFFILIATION)
                .orcid(USER_1_ORCID_URL)
                .build();

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            modify_generic(USER_4_USERNAME, USER_4, USER_4_PRINCIPAL, request);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"modify-user-information"})
    public void modify_hasRoleForeign_fails() {
        final UserUpdateDto request = UserUpdateDto.builder()
                .firstname(USER_1_FIRSTNAME)
                .lastname(USER_1_LASTNAME)
                .affiliation(USER_1_AFFILIATION)
                .orcid(USER_1_ORCID_URL)
                .build();

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            modify_generic(USER_1_USERNAME, USER_1, USER_2_PRINCIPAL, request);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"modify-user-information"})
    public void modify_succeeds() throws NotAllowedException, UserNotFoundException, AuthServiceException {
        final UserUpdateDto request = UserUpdateDto.builder()
                .firstname(USER_1_FIRSTNAME)
                .lastname(USER_1_LASTNAME)
                .affiliation(USER_1_AFFILIATION)
                .orcid(USER_1_ORCID_URL)
                .build();

        /* test */
        modify_generic(USER_1_USERNAME, USER_1, USER_1_PRINCIPAL, request);
    }

    @Test
    @WithAnonymousUser
    public void create_anonymous_fails() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            generic_create(USER_1_CREATE_USER_DTO);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME)
    public void create_notInternalUser_fails() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            generic_create(USER_1_CREATE_USER_DTO);
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void create_succeeds() {

        /* mock */
        when(userService.create(USER_1_CREATE_USER_DTO))
                .thenReturn(USER_1);

        /* test */
        generic_create(USER_1_CREATE_USER_DTO);
    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    protected List<UserBriefDto> findAll_generic(String username, User user) throws UserNotFoundException {

        /* mock */
        if (username != null) {
            if (user != null) {
                when(userService.findByUsername(username))
                        .thenReturn(user);
            } else {
                doThrow(UserNotFoundException.class)
                        .when(userService)
                        .findByUsername(username);
            }
        } else {
            when(userService.findAll())
                    .thenReturn(List.of(USER_1, USER_2, USER_LOCAL));
        }

        /* test */
        final ResponseEntity<List<UserBriefDto>> response = userEndpoint.findAll(username);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final List<UserBriefDto> body = response.getBody();
        assertNotNull(body);
        return response.getBody();
    }

    protected ResponseEntity<UserDto> find_generic(String username, User user, Principal principal) throws NotAllowedException,
            UserNotFoundException {

        /* mock */
        if (user != null) {
            when(userService.findByUsername(username))
                    .thenReturn(user);
        } else {
            doThrow(UserNotFoundException.class)
                    .when(userService)
                    .findByUsername(username);
        }

        /* test */
        final ResponseEntity<UserDto> response = userEndpoint.find(username, principal);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final UserDto body = response.getBody();
        assertNotNull(body);
        return response;
    }

    protected void modify_generic(String username, User user, Principal principal, UserUpdateDto data)
            throws NotAllowedException, UserNotFoundException, AuthServiceException {
        /* mock */
        if (user != null) {
            when(userService.findByUsername(username))
                    .thenReturn(user);
        }
        when(userService.modify(user, data))
                .thenReturn(user);

        /* test */
        final ResponseEntity<UserBriefDto> response = userEndpoint.modify(username, data, principal);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        final UserBriefDto body = response.getBody();
        assertNotNull(body);
    }

    protected void generic_create(CreateUserDto data) {

        /* test */
        final ResponseEntity<UserBriefDto> response = userEndpoint.create(data);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        final UserBriefDto body = response.getBody();
        assertNotNull(body);
    }
}
