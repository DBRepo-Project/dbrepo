package at.tuwien.gateway;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.user.UserDto;
import at.tuwien.config.IndexConfig;
import at.tuwien.config.ReadyConfig;
import at.tuwien.repository.jpa.UserRepository;
import com.rabbitmq.client.Channel;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;

import javax.servlet.ServletException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class AuthenticationServiceGatewayTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @MockBean
    private Channel channel;

    @MockBean
    private IndexConfig indexConfig;

    @MockBean
    @Qualifier("authenticationRestTemplate")
    private RestTemplate restTemplate;

    @MockBean
    private UserRepository userRepository;

    @Autowired
    private AuthenticationServiceGateway authenticationServiceGateway;

    @Test
    public void validate_succeeds() throws ServletException {
        final ResponseEntity<UserDto> mock = ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(USER_1_DTO);

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(UserDto.class)))
                .thenReturn(mock);

        /* test */
        final UserDetails response = authenticationServiceGateway.validate(JWT_1);
        assertNotNull(response);
        assertEquals(USER_1_USERNAME, response.getUsername());
        assertEquals(List.of(new SimpleGrantedAuthority("ROLE_RESEARCHER")), response.getAuthorities());
    }

    @Test
    public void validate_notFound_fails() {
        final ResponseEntity<UserDto> mock = ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(USER_1_DTO);

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(UserDto.class)))
                .thenReturn(mock);

        /* test */
        assertThrows(ServletException.class, () -> {
            authenticationServiceGateway.validate(JWT_1);
        });
    }

}
