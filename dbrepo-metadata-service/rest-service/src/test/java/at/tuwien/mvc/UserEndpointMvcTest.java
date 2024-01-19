package at.tuwien.mvc;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.api.auth.CreateUserDto;
import at.tuwien.api.auth.SignupRequestDto;
import at.tuwien.api.keycloak.UserCreateDto;
import at.tuwien.exception.BrokerRemoteException;
import at.tuwien.exception.KeycloakRemoteException;
import at.tuwien.gateway.BrokerServiceGateway;
import at.tuwien.gateway.KeycloakGateway;
import at.tuwien.gateway.impl.KeycloakGatewayImpl;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static at.tuwien.test.utils.ObjectUtil.asJsonString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Log4j2
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@SpringBootTest
@MockAmqp
@MockOpensearch
public class UserEndpointMvcTest extends BaseUnitTest {

    @MockBean
    private BrokerServiceGateway brokerServiceGateway;

    @MockBean
    private KeycloakGatewayImpl keycloakGateway;

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void createUser_malformed_fails() throws Exception {
        final SignupRequestDto request = SignupRequestDto.builder()
                .username(USER_1_USERNAME)
                .password(USER_1_PASSWORD)
                .email("invalid_email")
                .build();

        /* mock */
        doNothing()
                .when(brokerServiceGateway)
                .createUser(USER_1_USERNAME, USER_1_PASSWORD);

        /* test */
        this.mockMvc.perform(post("/api/user")
                        .content(asJsonString(request))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().is(400));
    }

    @Test
    public void createUser_keycloakOffline_503_fails() throws Exception {

        /* mock */
        doThrow(KeycloakRemoteException.class)
                .when(keycloakGateway)
                .createUser(any(UserCreateDto.class));

        /* test */
        this.mockMvc.perform(post("/api/user")
                        .content(asJsonString(USER_1_SIGNUP_REQUEST_DTO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().is(503));
    }

    @Test
    public void createUser_brokerOffline_503_fails() throws Exception {

        /* mock */
        doNothing()
                .when(keycloakGateway)
                .createUser(any(UserCreateDto.class));
        when(keycloakGateway.findByUsername(USER_1_USERNAME))
                .thenReturn(USER_1_KEYCLOAK_DTO);
        doThrow(BrokerRemoteException.class)
                .when(brokerServiceGateway)
                .createUser(USER_1_USERNAME, USER_1_PASSWORD);

        /* test */
        this.mockMvc.perform(post("/api/user")
                        .content(asJsonString(USER_1_SIGNUP_REQUEST_DTO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().is(503));
    }

}
