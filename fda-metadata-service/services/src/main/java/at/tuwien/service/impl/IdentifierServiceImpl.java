package at.tuwien.service.impl;

import at.tuwien.entities.identifier.Identifier;
import at.tuwien.exception.IdentifierNotFoundException;
import at.tuwien.repository.jpa.IdentifierRepository;
import at.tuwien.service.IdentifierService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional(readOnly = true)
    public List<Identifier> findAll() {
        return identifierRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Identifier find(Long id) throws IdentifierNotFoundException {
        final Optional<Identifier> optional = identifierRepository.findById(id);
        if (optional.isEmpty()) {
            log.error("Failed to find identifier with id {}", id);
            throw new IdentifierNotFoundException("Failed to find identifier");
        }
        return optional.get();
    }
}
