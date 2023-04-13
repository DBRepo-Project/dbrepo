package at.tuwien.auth;

import at.tuwien.BaseUnitTest;
import at.tuwien.config.H2Utils;
import at.tuwien.config.IndexInitializer;
import at.tuwien.config.ReadyConfig;
import at.tuwien.repository.jpa.UserRepository;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class AuthTokenFilterTest extends BaseUnitTest {

    @MockBean
    private IndexInitializer indexInitializer;

    @MockBean
    private ReadyConfig readyConfig;

    @MockBean
    private UserRepository userRepository;

    @Autowired
    private AuthTokenFilter authTokenFilter;

    @Autowired
    private H2Utils h2Utils;

    @BeforeEach
    public void beforeEach() {
        h2Utils.runScript("view.sql");
    }

    @Test
    public void parseJwt_fails() {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

        /* test */
        final String response = authTokenFilter.parseJwt(request);
        assertNull(response);
    }

    @Test
    public void parseJwt_noAuthenticationHeader_fails() {
        final MockHttpServletRequest request = new MockHttpServletRequest();

        /* test */
        final String response = authTokenFilter.parseJwt(request);
        assertNull(response);
    }

}
