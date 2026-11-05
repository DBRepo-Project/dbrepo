package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.exception.NotAllowedException;
import at.ac.tuwien.ifs.dbrepo.core.exception.UserNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.gateway.KeycloakGateway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class UserServiceAuthorityUnitTest extends BaseTest {

    @MockitoBean
    private KeycloakGateway keycloakGateway;

    @Autowired
    private UserService userService;

    @Test
    public void checkAuthority_systemUserWithAuthority_succeeds() throws UserNotFoundException, NotAllowedException {
        final UserRepresentation user = new UserRepresentation();
        user.setUsername(USER_1_USERNAME);
        user.setRealmRoles(List.of("system", "insert-table-data"));

        /* mock */
        when(keycloakGateway.findByUsername(USER_1_USERNAME))
                .thenReturn(user);

        /* test */
        userService.checkAuthority(USER_1_USERNAME, "insert-table-data");
    }

    @Test
    public void checkAuthority_systemAuthority_succeeds() throws UserNotFoundException, NotAllowedException {
        final UserRepresentation user = new UserRepresentation();
        user.setUsername(USER_1_USERNAME);
        user.setRealmRoles(List.of("system"));

        /* mock */
        when(keycloakGateway.findByUsername(USER_1_USERNAME))
                .thenReturn(user);

        /* test */
        userService.checkAuthority(USER_1_USERNAME, "system");
    }

    @Test
    public void checkAuthority_missingAuthority_fails() throws UserNotFoundException, NotAllowedException {
        final UserRepresentation user = new UserRepresentation();
        user.setUsername(USER_1_USERNAME);
        user.setRealmRoles(List.of("system"));

        /* mock */
        when(keycloakGateway.findByUsername(USER_1_USERNAME))
                .thenReturn(user);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            userService.checkAuthority(USER_1_USERNAME, "insert-table-data");
        });
    }

}
