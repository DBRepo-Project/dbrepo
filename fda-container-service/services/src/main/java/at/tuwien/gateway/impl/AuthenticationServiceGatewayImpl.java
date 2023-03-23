package at.tuwien.gateway.impl;

import at.tuwien.api.auth.TokenIntrospectDto;
import at.tuwien.api.user.UserBriefDto;
import at.tuwien.api.user.UserDetailsDto;
import at.tuwien.config.GatewayConfig;
import at.tuwien.gateway.AuthenticationServiceGateway;
import at.tuwien.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import javax.servlet.ServletException;

@Slf4j
@Service
public class AuthenticationServiceGatewayImpl implements AuthenticationServiceGateway {

    private final UserMapper userMapper;
    private final RestTemplate restTemplate;
    private final GatewayConfig gatewayConfig;

    @Autowired
    public AuthenticationServiceGatewayImpl(UserMapper userMapper, RestTemplate restTemplate, GatewayConfig gatewayConfig) {
        this.userMapper = userMapper;
        this.restTemplate = restTemplate;
        this.gatewayConfig = gatewayConfig;
    }

    @Override
    public UserDetails validate(String token) throws ServletException {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        final MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_secret", gatewayConfig.getClientSecret());
        body.add("client_id", gatewayConfig.getClientId());
        body.add("token", token);
        try {
            final ResponseEntity<TokenIntrospectDto> response = restTemplate.exchange("/api/auth/realms/dbrepo/protocol/openid-connect/token/introspect", HttpMethod.POST,
                    new HttpEntity<>(body, headers), TokenIntrospectDto.class);
            if (!response.getStatusCode().equals(HttpStatus.OK)) {
                log.error("Failed to validate token with status code {}", response.getStatusCode());
                throw new ServletException("Failed to validate token: http status code is not ok");
            } else if (response.getBody() == null) {
                throw new ServletException("Failed to validate token: body is null");
            } else if (!response.getBody().getActive()) {
                throw new ServletException("Failed to validate token: token is not active");
            }
            final UserDetailsDto dto = userMapper.tokenIntrospectDtoToUserDetailsDto(response.getBody());
            log.trace("gateway authenticated user {}", dto);
            return dto;
        } catch (HttpStatusCodeException e) {
            log.error("Failed to validate token with status code {}", e.getStatusCode());
            throw new ServletException("Failed to validate token", e);
        }
    }

}
