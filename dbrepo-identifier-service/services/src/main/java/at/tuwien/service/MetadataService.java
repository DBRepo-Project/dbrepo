package at.tuwien.service;

import at.tuwien.api.user.external.ExternalMetadataDto;
import at.tuwien.exception.OrcidNotFoundException;
import at.tuwien.exception.RemoteUnavailableException;
import at.tuwien.exception.RorNotFoundException;

public interface MetadataService {

    ExternalMetadataDto findByUrl(String url) throws OrcidNotFoundException, RorNotFoundException, RemoteUnavailableException;
}
