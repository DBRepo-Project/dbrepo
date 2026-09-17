package at.ac.tuwien.ifs.dbrepo.auth;

import at.ac.tuwien.ifs.dbrepo.config.GatewayConfig;
import at.ac.tuwien.ifs.dbrepo.gateway.KeycloakGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BasicAuthenticationProviderUnitTest {

    @Mock
    private JwtDecoder jwtDecoder;

    @Mock
    private GatewayConfig gatewayConfig;

    @Mock
    private KeycloakGateway keycloakGateway;

    @Mock
    private JwtAuthenticationConverter jwtAuthenticationConverter;

    private BasicAuthenticationProvider provider;

    @BeforeEach
    public void beforeEach() {
        provider = new BasicAuthenticationProvider(jwtDecoder, gatewayConfig, keycloakGateway,
                jwtAuthenticationConverter);
    }

    @Test
    public void authenticate_replicationCredentials_grantsReplicationAuthority() {
        when(gatewayConfig.getSystemUsername()).thenReturn("system");
        when(gatewayConfig.getReplicationUsername()).thenReturn("replication");
        when(gatewayConfig.getReplicationPassword()).thenReturn("secret");

        final Authentication authentication = provider.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated("replication", "secret"));

        assertEquals("replication", authentication.getAuthorities().iterator().next().getAuthority());
        verifyNoInteractions(keycloakGateway);
    }

    @Test
    public void authenticate_wrongReplicationPassword_rejectsWithoutUserLogin() {
        when(gatewayConfig.getSystemUsername()).thenReturn("system");
        when(gatewayConfig.getReplicationUsername()).thenReturn("replication");
        when(gatewayConfig.getReplicationPassword()).thenReturn("secret");

        assertThrows(BadCredentialsException.class, () -> provider.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated("replication", "wrong")));
        verifyNoInteractions(keycloakGateway);
    }

    @Test
    public void authenticate_systemCredentials_grantsSystemAuthority() {
        when(gatewayConfig.getSystemUsername()).thenReturn("system");
        when(gatewayConfig.getSystemPassword()).thenReturn("secret");

        final Authentication authentication = provider.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated("system", "secret"));

        assertEquals("system", authentication.getAuthorities().iterator().next().getAuthority());
        verifyNoInteractions(keycloakGateway);
    }
}
