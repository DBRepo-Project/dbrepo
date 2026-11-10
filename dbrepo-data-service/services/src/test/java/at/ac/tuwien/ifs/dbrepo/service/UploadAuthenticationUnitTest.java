package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.auth.BasicAuthenticationProvider;
import at.ac.tuwien.ifs.dbrepo.auth.BearerAuthenticationProvider;
import at.ac.tuwien.ifs.dbrepo.config.GatewayConfig;
import at.ac.tuwien.ifs.dbrepo.core.api.keycloak.TokenDto;
import at.ac.tuwien.ifs.dbrepo.core.storage.UploadMetadata;
import at.ac.tuwien.ifs.dbrepo.gateway.KeycloakGateway;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UploadAuthenticationUnitTest {
    @Test void basicAndBearerUseTheSameValidatedSubject() throws Exception {
        String subject = "b3468012-3375-4d80-b6b0-58b72f96594f";
        var jwt = Jwt.withTokenValue("signed-token").header("alg", "RS256").subject(subject)
                .claim("preferred_username", "min").claim("uid", "a-different-external-id").build();
        var decoder = mock(JwtDecoder.class);
        when(decoder.decode("signed-token")).thenReturn(jwt);
        var gateway = mock(KeycloakGateway.class);
        when(gateway.getUserToken("min", "password")).thenReturn(TokenDto.builder().accessToken("signed-token").build());
        var config = mock(GatewayConfig.class);
        when(config.getSystemUsername()).thenReturn("internal");
        var converter = new JwtAuthenticationConverter();
        var basic = new BasicAuthenticationProvider(decoder, config, gateway, converter)
                .authenticate(UsernamePasswordAuthenticationToken.unauthenticated("min", "password"));
        var bearer = new BearerAuthenticationProvider(decoder, converter)
                .authenticate(new BearerTokenAuthenticationToken("signed-token"));
        assertEquals(subject, UploadMetadata.ownerId(basic));
        assertEquals(subject, UploadMetadata.ownerId(bearer));
        verify(decoder, times(2)).decode("signed-token");
        verify(gateway).getUserToken("min", "password");
    }
}
