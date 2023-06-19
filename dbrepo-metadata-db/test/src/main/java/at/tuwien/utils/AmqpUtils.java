package at.tuwien.utils;

import at.tuwien.api.amqp.CreateUserDto;
import at.tuwien.api.amqp.ExchangeDto;
import at.tuwien.api.amqp.GrantVirtualHostPermissionsDto;
import at.tuwien.api.amqp.PermissionDto;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@Service
public class AmqpUtils {

    private final RestTemplate restTemplate;

    @Autowired
    public AmqpUtils(@Qualifier("brokerRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean exchangeExists(String exchange) {
        final String url = "/api/exchanges";
        log.debug("get exchange: {}", url);
        final ResponseEntity<ExchangeDto[]> response = restTemplate.exchange(url, HttpMethod.GET, null, ExchangeDto[].class);
        if (!response.getStatusCode().equals(HttpStatus.OK)) {
            log.error("Failed to retrieve exchanges, code is {}", response.getStatusCode());
            throw new RuntimeException("Failed to retrieve exchanges");
        }
        assert response.getBody() != null;
        final List<String> names = Arrays.stream(response.getBody())
                .map(ExchangeDto::getName)
                .collect(Collectors.toList());
        if (names.stream().filter(n -> n.equals(exchange)).count() != 1) {
            log.error("Failed to find exchange {} in exchanges {}", exchange, names);
            return false;
        }
        log.info("Found exchange {} in exchanges {}", exchange, names);
        return true;
    }

    public PermissionDto getPermissions(String username) {
        final String url = "/api/users/" + username + "/permissions";
        log.debug("get permissions: {}", url);
        final ResponseEntity<PermissionDto[]> response = restTemplate.exchange(
                "/api/users/" + username + "/permissions", HttpMethod.GET, null, PermissionDto[].class);
        if (!response.getStatusCode().equals(HttpStatus.OK)) {
            log.error("Failed to retrieve permissions: {}", response.getStatusCode());
            throw new RuntimeException("Failed to retrieve permissions: {}" + response.getStatusCode());
        }
        assert response.getBody() != null;
        if (response.getBody().length != 1) {
            log.error("Failed to retrieve permissions: expecting exactly one result");
            throw new RuntimeException("Failed to retrieve permissions: expecting exactly one result");
        }
        log.trace("found permissions: {}", response.getBody()[0]);
        return response.getBody()[0];
    }

    public void createUser(String username, CreateUserDto data) {
        final String url = "/api/users/" + username;
        log.debug("add user: {}", url);
        log.trace("body: {}", data);
        final ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(data), Void.class);
        if (!response.getStatusCode().equals(HttpStatus.CREATED) && !response.getStatusCode().equals(HttpStatus.NO_CONTENT)) {
            log.error("Failed to create user: {}", response.getStatusCode());
            throw new RuntimeException("Failed to create user: {}" + response.getStatusCode());
        }
    }

    public void setPermissions(String endpoint, String vhost, String username, GrantVirtualHostPermissionsDto data) {
        final URI url = URI.create(endpoint + "/api/permissions/" + vhost + "/" + username);
        log.debug("set user permissions: {}", url);
        log.trace("body: {}", data);
        final MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Authentication", "Basic " + new String(Base64.encodeBase64("guest:guest".getBytes(Charset.defaultCharset()))));
        final ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(data, headers), Void.class);
        if (!response.getStatusCode().equals(HttpStatus.CREATED) && !response.getStatusCode().equals(HttpStatus.NO_CONTENT)) {
            log.error("Failed to set user permissions: {}", response.getStatusCode());
            throw new RuntimeException("Failed to set user permissions: {}" + response.getStatusCode());
        }
    }

}
