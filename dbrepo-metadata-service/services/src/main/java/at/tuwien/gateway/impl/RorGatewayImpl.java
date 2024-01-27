package at.tuwien.gateway.impl;

import at.tuwien.api.ror.RorDto;
import at.tuwien.exception.RorNotFoundException;
import at.tuwien.gateway.RorGateway;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Log4j2
@Service
public class RorGatewayImpl implements RorGateway {

    private final RestTemplate restTemplate;

    public RorGatewayImpl() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public RorDto findById(String id) throws RorNotFoundException {
        final HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        final String url = "https://api.ror.org/organizations/" + id;
        final ResponseEntity<RorDto> response;
        try {
            log.trace("find ror from url {}", url);
            response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(null, headers), RorDto.class);
        } catch (ResourceAccessException | HttpServerErrorException.ServiceUnavailable e) {
            log.error("Failed to retrieve ROR metadata from URL {}: {}", url, e.getMessage());
            throw new RorNotFoundException("Failed to retrieve ROR metadata from URL " + url + ": " + e.getMessage(), e);
        }
        return response.getBody();
    }

}
