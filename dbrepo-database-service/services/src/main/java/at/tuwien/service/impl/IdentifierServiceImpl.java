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
    public List<Identifier> findAll(Long databaseId) {
        return identifierRepository.findByDatabaseId(databaseId);
    }

}
