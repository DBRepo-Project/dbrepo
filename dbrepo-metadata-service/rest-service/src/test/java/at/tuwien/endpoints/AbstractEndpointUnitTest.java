package at.tuwien.endpoints;

import at.tuwien.api.database.AccessTypeDto;
import at.tuwien.api.database.DatabaseAccessDto;
import at.tuwien.api.user.UserDetailsDto;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.mapper.MetadataMapper;
import at.tuwien.service.AccessService;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.UserService;
import at.tuwien.test.AbstractUnitTest;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class AbstractEndpointUnitTest extends AbstractUnitTest {

    @Autowired
    private AccessEndpoint accessEndpoint;

    @BeforeEach
    public void beforeEach() {
        genesis();
    }

    @Test
    public void hasRole_noPrincipal_fails() {

        /* test */
        assertFalse(accessEndpoint.hasRole(null, "some-role"));
    }

    @Test
    public void hasRole_noRole_fails() {

        /* test */
        assertFalse(accessEndpoint.hasRole(USER_1_PRINCIPAL, null));
    }

    @Test
    public void getId_fails() {

        /* test */
        assertNull(accessEndpoint.getId(null));
    }

    @Test
    public void getId_noId_fails() {
        final Principal principal = new UsernamePasswordAuthenticationToken(UserDetailsDto.builder()
                .id(null) // <<<
                .build(), null);

        /* test */
        assertThrows(IllegalArgumentException.class, () -> {
            accessEndpoint.getId(principal);
        });
    }

    @Test
    public void getId_incompatible_fails() {
        final Principal principal = new UsernamePasswordAuthenticationToken("", null);

        /* test */
        assertThrows(IllegalArgumentException.class, () -> {
            accessEndpoint.getId(principal);
        });
    }

}
