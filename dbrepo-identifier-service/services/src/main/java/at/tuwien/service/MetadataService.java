package at.tuwien.service;

import at.tuwien.api.user.external.ExternalMetadataDto;
import at.tuwien.exception.DoiNotFoundException;
import at.tuwien.exception.OrcidNotFoundException;
import at.tuwien.exception.RemoteUnavailableException;
import at.tuwien.exception.RorNotFoundException;

public interface MetadataService {

    /**
     * Finds creator user metadata by remote service and user identifier.
     *
     * @param url The user identifier.
     * @return The user metadata.
     * @throws OrcidNotFoundException     The provided identifier is of ORCID type and does not exist.
     * @throws RorNotFoundException       The provided identifier is of ROR type and does not exist.
     * @throws RemoteUnavailableException The remote service is not supported.
     */
    ExternalMetadataDto findByUrl(String url) throws OrcidNotFoundException, RorNotFoundException, RemoteUnavailableException, DoiNotFoundException;
}
