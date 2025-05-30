package at.ac.tuwien.ifs.dbrepo.utils;

import at.ac.tuwien.ifs.dbrepo.core.api.amqp.*;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class AmqpUtils extends BaseTest {

    private static final String BASIC_AUTH = new String(Base64.encodeBase64((USER_1_USERNAME + ":" + USER_1_PASSWORD).getBytes(Charset.defaultCharset())));

    public static boolean exchangeExists(RestTemplate restTemplate, String exchange) {
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

    public static VirtualHostPermissionDto getVirtualHostPermissions(RestTemplate restTemplate, String username) {
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

    public static TopicPermissionDto getTopicPermissions(RestTemplate restTemplate, String username) {
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

    public static void createUser(RestTemplate restTemplate, String username, CreateUserDto data) {
        final String url = "/api/users/" + username;
        log.debug("add user: {}", url);
        log.trace("body: {}", data);
        final ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(data), Void.class);
        if (!response.getStatusCode().equals(HttpStatus.CREATED) && !response.getStatusCode().equals(HttpStatus.NO_CONTENT)) {
            log.error("Failed to create user: {}", response.getStatusCode());
            throw new RuntimeException("Failed to create user: {}" + response.getStatusCode());
        }
    }

    public static void setVirtualHostPermissions(RestTemplate restTemplate, String vhost, String username, GrantVirtualHostPermissionsDto data) {
        final String url = "/api/permissions/" + vhost + "/" + username;
        log.trace("body: {}", data);
        final MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Authentication", "Basic " + BASIC_AUTH);
        log.trace("set virtual host permissions: {}", url);
        final ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(data, headers), Void.class);
        if (!response.getStatusCode().equals(HttpStatus.CREATED) && !response.getStatusCode().equals(HttpStatus.NO_CONTENT)) {
            log.error("Failed to set virtual host permissions: {}", response.getStatusCode());
            throw new RuntimeException("Failed to set virtual host permissions: {}" + response.getStatusCode());
        }
    }

    public static void setTopicPermissions(RestTemplate restTemplate, String vhost, String username, GrantExchangePermissionsDto data) {
        final String url = "/api/topic-permissions/" + vhost + "/" + username;
        log.debug("set topic permissions: {}", url);
        log.trace("body: {}", data);
        final MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Authentication", "Basic " + BASIC_AUTH);
        final ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(data, headers), Void.class);
        if (!response.getStatusCode().equals(HttpStatus.CREATED) && !response.getStatusCode().equals(HttpStatus.NO_CONTENT)) {
            log.error("Failed to set topic permissions: {}", response.getStatusCode());
            throw new RuntimeException("Failed to set topic permissions: {}" + response.getStatusCode());
        }
    }

    public static boolean queueExists(RestTemplate restTemplate, String queue) {
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
