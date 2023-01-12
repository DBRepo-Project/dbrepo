package at.tuwien.endpoint;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.user.RoleTypeDto;
import at.tuwien.api.user.UserDto;
import at.tuwien.api.user.UserRolesDto;
import at.tuwien.endpoints.UserEndpoint;
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
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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

        /* mock */

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

        /* mock */

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

        /* mock */

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

        /* mock */

        /* test */
        updateRoles_generic(USER_3_ID, USER_3, request);
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


}
