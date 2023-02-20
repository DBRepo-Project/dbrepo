package at.tuwien.service;

import at.tuwien.entities.identifier.Identifier;
import at.tuwien.entities.identifier.IdentifierType;
import at.tuwien.exception.IdentifierNotFoundException;
import org.jvnet.hk2.annotations.Service;

import java.util.List;

@Service
public interface IdentifierService {
    List<Identifier> findAll(Long containerId);

    Identifier find(Long containerId, Long databaseId, IdentifierType type) throws IdentifierNotFoundException;
}
