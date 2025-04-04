package at.ac.tuwien.ifs.dbrepo.gateway.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.orcid.OrcidDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.OrcidNotFoundException;
import at.ac.tuwien.ifs.dbrepo.gateway.OrcidGateway;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

@Log4j2
@Service
public class OrcidGatewayImpl implements OrcidGateway {

    private final RestTemplate restTemplate;

    @Autowired
    public OrcidGatewayImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public OrcidDto findByUrl(String url) throws OrcidNotFoundException {
        log.trace("find orcid by url at endpoint {}", url);
        final ResponseEntity<OrcidDto> response;
        try {
            response = restTemplate.exchange(url, HttpMethod.GET, HttpEntity.EMPTY, OrcidDto.class);
        } catch (HttpServerErrorException e) {
            log.error("Failed to retrieve orcid metadata: {}", e.getMessage());
            throw new OrcidNotFoundException("Failed to retrieve orcid metadata: " + e.getMessage());
        }
        return response.getBody();
    }
}