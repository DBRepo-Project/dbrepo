package at.tuwien.service.impl;

import at.tuwien.entities.identifier.Identifier;
import at.tuwien.exception.IdentifierNotFoundException;
import at.tuwien.repository.mdb.IdentifierRepository;
import at.tuwien.service.IdentifierService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class IdentifierServiceImpl implements IdentifierService {

    private final IdentifierRepository identifierRepository;

    @Autowired
    public IdentifierServiceImpl(IdentifierRepository identifierRepository) {
        this.identifierRepository = identifierRepository;
    }

    @Override
    public Identifier findByDatabaseIdAndQueryId(Long databaseId, Long queryId) throws IdentifierNotFoundException {
        final Optional<Identifier> optional = identifierRepository.findByDatabaseIdAndQueryId(databaseId, queryId);
        if (optional.isEmpty()) {
            throw new IdentifierNotFoundException("Failed to find identifier");
        }
        return optional.get();
    }

    @Override
    public List<Identifier> findAll() {
        return identifierRepository.findAll();
    }
}
