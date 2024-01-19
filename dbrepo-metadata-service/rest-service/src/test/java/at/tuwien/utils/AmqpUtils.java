package at.tuwien.utils;

import at.tuwien.api.amqp.*;
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

    public VirtualHostPermissionDto getVirtualHostPermissions(String username) {
        final String url = "/api/users/" + username + "/permissions";
        log.debug("get virtual host permissions: {}", url);
        final ResponseEntity<VirtualHostPermissionDto[]> response = restTemplate.exchange(url, HttpMethod.GET, null, VirtualHostPermissionDto[].class);
        if (!response.getStatusCode().equals(HttpStatus.OK)) {
            log.error("Failed to retrieve virtual host  permissions: {}", response.getStatusCode());
            throw new RuntimeException("Failed to retrieve virtual host  permissions: {}" + response.getStatusCode());
        }
        assert response.getBody() != null;
        if (response.getBody().length != 1) {
            log.error("Failed to retrieve virtual host permissions: expecting exactly one result");
            throw new RuntimeException("Failed to retrieve virtual host permissions: expecting exactly one result");
        }
        log.trace("found virtual host permissions: {}", response.getBody()[0]);
        return response.getBody()[0];
    }

    public TopicPermissionDto getTopicPermissions(String username) {
        final String url = "/api/topic-permissions/dbrepo/" + username;
        log.debug("get topic permissions: {}", url);
        final ResponseEntity<TopicPermissionDto[]> response = restTemplate.exchange(url, HttpMethod.GET, null, TopicPermissionDto[].class);
        if (!response.getStatusCode().equals(HttpStatus.OK)) {
            log.error("Failed to retrieve topic permissions: {}", response.getStatusCode());
            throw new RuntimeException("Failed to retrieve topic permissions: {}" + response.getStatusCode());
        }
        assert response.getBody() != null;
        if (response.getBody().length != 1) {
            log.error("Failed to retrieve topic permissions: expecting exactly one result");
            throw new RuntimeException("Failed to retrieve topic permissions: expecting exactly one result");
        }
        log.trace("found topic permissions: {}", response.getBody()[0]);
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

    public void setVirtualHostPermissions(String vhost, String username, GrantVirtualHostPermissionsDto data) {
        final String url = "/api/permissions/" + vhost + "/" + username;
        log.debug("set virtual host permissions: {}", url);
        log.trace("body: {}", data);
        final MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Authentication", "Basic " + new String(Base64.encodeBase64("guest:guest".getBytes(Charset.defaultCharset()))));
        final ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(data, headers), Void.class);
        if (!response.getStatusCode().equals(HttpStatus.CREATED) && !response.getStatusCode().equals(HttpStatus.NO_CONTENT)) {
            log.error("Failed to set virtual host permissions: {}", response.getStatusCode());
            throw new RuntimeException("Failed to set virtual host permissions: {}" + response.getStatusCode());
        }
    }

    public void setTopicPermissions(String vhost, String username, GrantExchangePermissionsDto data) {
        final String url = "/api/topic-permissions/" + vhost + "/" + username;
        log.debug("set topic permissions: {}", url);
        log.trace("body: {}", data);
        final MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Authentication", "Basic " + new String(Base64.encodeBase64("guest:guest".getBytes(Charset.defaultCharset()))));
        final ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(data, headers), Void.class);
        if (!response.getStatusCode().equals(HttpStatus.CREATED) && !response.getStatusCode().equals(HttpStatus.NO_CONTENT)) {
            log.error("Failed to set topic permissions: {}", response.getStatusCode());
            throw new RuntimeException("Failed to set topic permissions: {}" + response.getStatusCode());
        }
    }

    public boolean queueExists(String queue) {
        final ResponseEntity<QueueDto[]> response = restTemplate.exchange("/api/queues/{1}/", HttpMethod.GET, new HttpEntity<>(null), QueueDto[].class, "/");
        if (!response.getStatusCode().equals(HttpStatus.OK)) {
            log.error("Failed to find queue, code is {}", response.getStatusCode());
            throw new RuntimeException("Failed to find queue");
        }
        assert response.getBody() != null;
        final List<String> names = Arrays.stream(response.getBody())
                .map(QueueDto::getName)
                .collect(Collectors.toList());
        if (names.stream().filter(n -> n.equals(queue)).count() != 1) {
            log.error("Failed to find queue {} in queues {}", queue, names);
            return false;
        }
        log.info("Found queue {} in queues {}", queue, names);
        return true;
    }

}
