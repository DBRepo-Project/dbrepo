package at.tuwien.service;

import at.tuwien.api.user.UserOrOrganisationIdentifierDto;
import at.tuwien.exception.OrcidNotFoundException;

public interface MetadataService {

    UserOrOrganisationIdentifierDto findById(UserOrOrganisationIdentifierDto data) throws OrcidNotFoundException;
}
