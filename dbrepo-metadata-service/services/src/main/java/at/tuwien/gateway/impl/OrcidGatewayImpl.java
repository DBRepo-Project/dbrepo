package at.tuwien.gateway.impl;

import at.tuwien.api.orcid.OrcidDto;
import at.tuwien.exception.OrcidNotFoundException;
import at.tuwien.gateway.OrcidGateway;
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
public class OrcidGatewayImpl implements OrcidGateway {

    private final RestTemplate restTemplate;

    public OrcidGatewayImpl() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public OrcidDto findByUrl(String url) throws OrcidNotFoundException {
        final HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        final ResponseEntity<OrcidDto> response;
        try {
            log.debug("find orcid from url {}", url);
            response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(null, headers), OrcidDto.class);
        } catch (ResourceAccessException | HttpServerErrorException.ServiceUnavailable e) {
            log.error("Failed to retrieve ORCID metadata from URL {}: {}", url, e.getMessage());
            throw new OrcidNotFoundException("Failed to retrieve ORCID metadata from URL " + url + ": " + e.getMessage());
        }
        return response.getBody();
    }
}