package at.tuwien.service.impl;

import at.tuwien.entities.identifier.Identifier;
import at.tuwien.entities.identifier.IdentifierType;
import at.tuwien.exception.IdentifierNotFoundException;
import at.tuwien.repository.jpa.IdentifierRepository;
import at.tuwien.service.IdentifierService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Log4j2
@Service
public class IdentifierServiceImpl implements IdentifierService {

    private final IdentifierRepository identifierRepository;

    @Autowired
    public IdentifierServiceImpl(IdentifierRepository identifierRepository) {
        this.identifierRepository = identifierRepository;
    }

    @Override
    public List<Identifier> findAll(Long containerId) {
        return identifierRepository.findByContainerId(containerId);
    }

    @Override
    public Identifier find(Long containerId, Long databaseId, IdentifierType type) throws IdentifierNotFoundException {
        final Optional<Identifier> optional = identifierRepository.findByContainerIdAndDatabaseIdAndType(containerId, databaseId, type);
        if (optional.isEmpty()) {
            log.error("Failed to find identifier with container id {} and database id {} and type {}", containerId, databaseId, type);
            throw new IdentifierNotFoundException("Failed to find identifier");
        }
        return optional.get();
    }

}
