package at.ac.tuwien.ifs.dbrepo.utils;

import at.ac.tuwien.ifs.dbrepo.core.api.user.UserDetailsDto;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.security.Principal;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class AuthUtilUnitTest extends BaseTest {

    @Test
    public void hasRole_noPrincipal_fails() {

        /* test */
        assertFalse(AuthUtil.hasRole(null, "some-role"));
    }

    @Test
    public void hasRole_noRole_fails() {

        /* test */
        assertFalse(AuthUtil.hasRole(USER_1_PRINCIPAL, null));
    }

    @Test
    public void getUsername_fails() {

        /* test */
        assertNull(AuthUtil.getUsername(null));
    }

    @Test
    public void getUsername_noId_fails() {
        final Principal principal = new UsernamePasswordAuthenticationToken(UserDetailsDto.builder()
                .id(null) // <<<
                .build(), null);

        /* test */
        assertThrows(IllegalArgumentException.class, () -> {
            AuthUtil.getUsername(principal);
        });
    }

    @Test
    public void getUsername_incompatible_fails() {
        final Principal principal = new UsernamePasswordAuthenticationToken("", null);

        /* test */
        assertThrows(IllegalArgumentException.class, () -> {
            AuthUtil.getUsername(principal);
        });
    }

}
