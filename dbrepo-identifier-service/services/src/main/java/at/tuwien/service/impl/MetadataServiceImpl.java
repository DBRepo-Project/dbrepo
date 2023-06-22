package at.tuwien.service.impl;

import at.tuwien.api.user.UserOrOrganisationIdentifierDto;
import at.tuwien.exception.OrcidNotFoundException;
import at.tuwien.gateway.OrcidGateway;
import at.tuwien.service.MetadataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MetadataServiceImpl implements MetadataService {

    private final OrcidGateway orcidGateway;

    @Autowired
    public MetadataServiceImpl(OrcidGateway orcidGateway) {
        this.orcidGateway = orcidGateway;
    }

    @Override
    public UserOrOrganisationIdentifierDto findById(UserOrOrganisationIdentifierDto data) throws OrcidNotFoundException {
        orcidGateway.findByUrl(data.getUrl());
        return null;
    }

}
