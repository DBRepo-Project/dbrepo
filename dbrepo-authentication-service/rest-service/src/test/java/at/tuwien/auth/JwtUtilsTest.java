package at.tuwien.auth;

import at.tuwien.BaseUnitTest;
import at.tuwien.config.ReadyConfig;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class JwtUtilsTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @Autowired
    private JwtUtils jwtUtils;

    @Test
    public void getUserNameFromJwtToken_succeeds() {

        /* test */
        final String response = jwtUtils.getUserNameFromJwtToken(JWT_1);
        assertEquals("mweise", response);
    }

}
