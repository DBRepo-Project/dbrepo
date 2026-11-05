package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.exception.NotAllowedException;
import at.ac.tuwien.ifs.dbrepo.core.exception.UserNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.gateway.KeycloakGateway;
import at.ac.tuwien.ifs.dbrepo.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@ExtendWith(SpringExtension.class)
public class UserEndpointAuthorityUnitTest extends BaseTest {

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private KeycloakGateway keycloakGateway;

    @Autowired
    private UserEndpoint userEndpoint;

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void checkAuthority_system_succeeds() throws UserNotFoundException, NotAllowedException {

        /* mock */
        doNothing()
                .when(userService)
                .checkAuthority(USER_1_USERNAME, "insert-table-data");

        /* test */
        final ResponseEntity<Void> response = userEndpoint.checkAuthority(USER_1_USERNAME, "insert-table-data");
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void checkAuthority_noRole_fails() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            userEndpoint.checkAuthority(USER_1_USERNAME, "insert-table-data");
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void checkAuthority_missingAuthority_fails() throws UserNotFoundException, NotAllowedException {

        /* mock */
        doThrow(NotAllowedException.class)
                .when(userService)
                .checkAuthority(USER_1_USERNAME, "insert-table-data");

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            userEndpoint.checkAuthority(USER_1_USERNAME, "insert-table-data");
        });
    }

}
