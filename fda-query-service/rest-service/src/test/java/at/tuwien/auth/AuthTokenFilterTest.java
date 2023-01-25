package at.tuwien.auth;

import at.tuwien.BaseUnitTest;
import at.tuwien.config.H2Utils;
import at.tuwien.config.ReadyConfig;
import at.tuwien.gateway.AuthenticationServiceGateway;
import at.tuwien.repository.jpa.UserRepository;
import com.rabbitmq.client.Channel;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class AuthTokenFilterTest extends BaseUnitTest {

    @MockBean
    private Channel channel;

    @MockBean
    private ReadyConfig readyConfig;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private AuthenticationServiceGateway authenticationServiceGateway;

    @Autowired
    private AuthTokenFilter authTokenFilter;

    @Autowired
    private H2Utils h2Utils;

    @BeforeEach
    public void beforeEach() {
//        h2Utils.runScript("schema.sql");
    }

    @Test
    public void doFilterInternal_notFound_fails() throws ServletException {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + JWT_1);
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final FilterChain chain = new MockFilterChain();

        /* mock */
        doThrow(new ServletException("Username not found"))
                .when(authenticationServiceGateway)
                .validate(anyString());
        when(userRepository.findByUsername("mweise"))
                .thenReturn(Optional.empty());

        /* test */
        assertThrows(ServletException.class, () -> {
            authTokenFilter.doFilterInternal(request, response, chain);
        });
    }

    @Test
    public void doFilterInternal_succeeds() throws ServletException, IOException {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + JWT_1);
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final FilterChain chain = new MockFilterChain();

        /* mock */
        when(authenticationServiceGateway.validate(anyString()))
                .thenReturn(USER_1_DETAILS);
        when(userRepository.findByUsername("mweise"))
                .thenReturn(Optional.of(USER_1));

        /* test */
        authTokenFilter.doFilterInternal(request, response, chain);
        assertEquals(200, response.getStatus());
    }

    @Test
    public void parseJwt_succeeds() {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + JWT_1);

        /* test */
        final String response = authTokenFilter.parseJwt(request);
        assertEquals(JWT_1, response);
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
