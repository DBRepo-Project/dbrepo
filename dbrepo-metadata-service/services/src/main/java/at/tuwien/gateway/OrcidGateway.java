package at.tuwien.gateway;

import at.tuwien.api.orcid.OrcidDto;
import at.tuwien.exception.OrcidNotFoundException;
import org.springframework.stereotype.Service;

@Service
public interface OrcidGateway {

    OrcidDto findByUrl(String url) throws OrcidNotFoundException;
}
