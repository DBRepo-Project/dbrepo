package at.tuwien.gateway;

import at.tuwien.api.orcid.OrcidDto;
import at.tuwien.exception.OrcidNotFoundException;
import org.springframework.stereotype.Service;

@Service
public interface OrcidGateway {

    /**
     * Finds metadata from given ORCID url.
     * @param url The ORCID url.
     * @return The metadata, if successful.
     * @throws OrcidNotFoundException The metadata does not exist to the given ORCID.
     */
    OrcidDto findByUrl(String url) throws OrcidNotFoundException;
}
