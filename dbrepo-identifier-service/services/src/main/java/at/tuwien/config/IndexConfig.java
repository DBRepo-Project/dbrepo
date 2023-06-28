package at.tuwien.config;

import at.tuwien.entities.identifier.Identifier;
import at.tuwien.repository.sdb.IdentifierIdxRepository;
import at.tuwien.repository.mdb.IdentifierRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Log4j2
public class IndexConfig {

    private final IdentifierRepository identifierRepository;
    private final IdentifierIdxRepository identifierIdxRepository;

    @Autowired
    public IndexConfig(IdentifierRepository identifierRepository, IdentifierIdxRepository identifierIdxRepository) {
        this.identifierRepository = identifierRepository;
        this.identifierIdxRepository = identifierIdxRepository;
    }

    @Transactional
    @EventListener(ApplicationReadyEvent.class)
    public void initIndex() {
        final List<Identifier> identifiers = identifierRepository.findAll();
        identifierIdxRepository.saveAll(identifiers);
        log.info("Added {} identifiers to open search index", identifiers.size());
    }
}
