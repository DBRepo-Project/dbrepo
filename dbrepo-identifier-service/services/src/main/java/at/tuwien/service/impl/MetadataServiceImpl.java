package at.tuwien.service.impl;

import at.tuwien.api.ror.RorDto;
import at.tuwien.api.user.external.ExternalMetadataDto;
import at.tuwien.exception.OrcidNotFoundException;
import at.tuwien.exception.RemoteUnavailableException;
import at.tuwien.exception.RorNotFoundException;
import at.tuwien.gateway.OrcidGateway;
import at.tuwien.gateway.RorGateway;
import at.tuwien.mapper.ExternalMapper;
import at.tuwien.service.MetadataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MetadataServiceImpl implements MetadataService {

    private final RorGateway rorGateway;
    private final OrcidGateway orcidGateway;
    private final ExternalMapper externalMapper;

    @Autowired
    public MetadataServiceImpl(RorGateway rorGateway, OrcidGateway orcidGateway, ExternalMapper externalMapper) {
        this.rorGateway = rorGateway;
        this.orcidGateway = orcidGateway;
        this.externalMapper = externalMapper;
    }

    @Override
    public ExternalMetadataDto findByUrl(String url) throws OrcidNotFoundException, RorNotFoundException,
            RemoteUnavailableException {
        if (url.contains("orcid.org")) {
            return externalMapper.orcidDtoToExternalMetadataDto(orcidGateway.findByUrl(url));
        } else if (url.contains("ror.org")) {
            final int idx = url.lastIndexOf('/');
            if (idx + 1 >= url.length()) {
                log.error("Failed to find metadata from ROR URL: too short");
                throw new RorNotFoundException("Failed to find metadata from ROR URL: too short");
            }
            final String id = url.substring(idx + 1);
            return externalMapper.rorDtoToExternalMetadataDto(rorGateway.findById(id));
        }
        log.error("Failed to find metadata: unsupported identifier");
        throw new RemoteUnavailableException("Failed to find metadata: unsupported identifier");
    }

}
