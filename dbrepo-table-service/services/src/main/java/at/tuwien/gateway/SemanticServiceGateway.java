package at.tuwien.gateway;

import at.tuwien.api.semantics.EntityDto;
import at.tuwien.exception.SemanticEntityNotFoundException;

public interface SemanticServiceGateway {
    EntityDto getEntity(Long ontologyId, String uri, String authorization) throws SemanticEntityNotFoundException;
}
