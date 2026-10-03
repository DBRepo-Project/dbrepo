package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.auth.BasicAuthenticationProvider;
import at.ac.tuwien.ifs.dbrepo.auth.BearerAuthenticationProvider;
import at.ac.tuwien.ifs.dbrepo.config.GatewayConfig;
import at.ac.tuwien.ifs.dbrepo.config.WebSecurityConfig;
import at.ac.tuwien.ifs.dbrepo.gateway.KeycloakGateway;
import at.ac.tuwien.ifs.dbrepo.service.CredentialService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.net.SocketTimeoutException;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitWebConfig({BrokerHealthEndpointUnitTest.Config.class, WebSecurityConfig.class})
class BrokerHealthEndpointUnitTest {

    private static final String PATH = "/api/metadata/broker/health";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    @Qualifier("brokerRestTemplate")
    private RestTemplate brokerRestTemplate;

    private MockMvc mvc;
    private MockRestServiceServer broker;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        broker = MockRestServiceServer.bindTo(brokerRestTemplate).build();
    }

    @AfterEach
    void verifyRequests() {
        broker.verify();
    }

    @ParameterizedTest
    @CsvSource({
            "200, ok, UP",
            "503, failed, DOWN",
            "401, denied, UNAVAILABLE",
            "403, denied, UNAVAILABLE",
            "500, error, UNAVAILABLE",
            "503, proxy-error, UNAVAILABLE",
            "404, missing, UNKNOWN",
            "200, unexpected, UNKNOWN",
            "200, failed, UNKNOWN"
    })
    void health_preservesProbeOutcome(int httpStatus, String rabbitStatus, String expected) throws Exception {
        broker.expect(requestTo("http://broker.test/api/health/checks/alarms"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Basic " + HttpHeaders.encodeBasicAuth(
                        "broker-admin", "broker-password", null)))
                .andRespond(withStatus(HttpStatus.valueOf(httpStatus)).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"status\":\"" + rabbitStatus + "\"}"));

        mvc.perform(get(PATH).with(httpBasic("system", "test-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("broker"))
                .andExpect(jsonPath("$.status").value(expected))
                .andExpect(jsonPath("$.http_status").value(httpStatus))
                .andExpect(jsonPath("$.duration_ms").value(greaterThanOrEqualTo(0)));
    }

    @ParameterizedTest
    @CsvSource({"200, UNKNOWN", "503, UNAVAILABLE"})
    void health_doesNotTrustProxyHtml(int httpStatus, String expected) throws Exception {
        broker.expect(requestTo("http://broker.test/api/health/checks/alarms"))
                .andRespond(withStatus(HttpStatus.valueOf(httpStatus)).contentType(MediaType.TEXT_HTML)
                        .body("<html>upstream unavailable</html>"));
        mvc.perform(get(PATH).with(httpBasic("system", "test-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(expected))
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "null", "{}", "[]", "not-json"})
    void health_invalidResponseIsUnknown(String body) throws Exception {
        broker.expect(requestTo("http://broker.test/api/health/checks/alarms"))
                .andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body(body));
        mvc.perform(get(PATH).with(httpBasic("system", "test-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNKNOWN"));
    }

    @Test
    void health_timeoutIsUnavailableWithoutLeakingException() throws Exception {
        broker.expect(requestTo("http://broker.test/api/health/checks/alarms"))
                .andRespond(withException(new SocketTimeoutException("sensitive connection detail")));
        mvc.perform(get(PATH).with(httpBasic("system", "test-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNAVAILABLE"))
                .andExpect(jsonPath("$.http_status").doesNotExist())
                .andExpect(jsonPath("$.error").value("Broker management endpoint is unreachable or timed out"));
    }

    @Test
    void health_requiresAuthentication() throws Exception {
        mvc.perform(get(PATH)).andExpect(status().isUnauthorized());
        mvc.perform(get(PATH).with(httpBasic("system", "wrong-password")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void health_requiresSystemAuthority() throws Exception {
        mvc.perform(get(PATH).with(user("ordinary"))).andExpect(status().isForbidden());
        mvc.perform(get(PATH).with(user("replication").authorities(new SimpleGrantedAuthority("replication"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void brokerClient_hasBoundedTimeouts() {
        final RestTemplate template = gatewayConfig().brokerRestTemplate();
        final Object factory = ReflectionTestUtils.getField(template, "requestFactory");
        assertEquals(5_000, ReflectionTestUtils.getField(factory, "connectTimeout"));
        assertEquals(5_000, ReflectionTestUtils.getField(factory, "readTimeout"));
    }

    private static GatewayConfig gatewayConfig() {
        final GatewayConfig config = new GatewayConfig(mock(CredentialService.class));
        ReflectionTestUtils.setField(config, "brokerEndpoint", "http://broker.test");
        ReflectionTestUtils.setField(config, "brokerUsername", "broker-admin");
        ReflectionTestUtils.setField(config, "brokerPassword", "broker-password");
        ReflectionTestUtils.setField(config, "systemUsername", "system");
        ReflectionTestUtils.setField(config, "systemPassword", "test-password");
        ReflectionTestUtils.setField(config, "replicationUsername", "replication");
        return config;
    }

    @Configuration
    @EnableWebMvc
    static class Config {

        @Bean("brokerRestTemplate")
        RestTemplate brokerRestTemplate() {
            return gatewayConfig().brokerRestTemplate();
        }

        @Bean
        BrokerHealthEndpoint endpoint(@Qualifier("brokerRestTemplate") RestTemplate template) {
            return new BrokerHealthEndpoint(template);
        }

        @Bean
        JwtDecoder jwtDecoder() {
            return mock(JwtDecoder.class);
        }

        @Bean
        BasicAuthenticationProvider basicAuthenticationProvider(JwtDecoder decoder,
                                                               JwtAuthenticationConverter converter) {
            return new BasicAuthenticationProvider(decoder, gatewayConfig(), mock(KeycloakGateway.class), converter);
        }

        @Bean
        BearerAuthenticationProvider bearerAuthenticationProvider() {
            return mock(BearerAuthenticationProvider.class);
        }
    }
}
