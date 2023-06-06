package at.tuwien.config;

import at.tuwien.api.identifier.IdentifierDto;
import at.tuwien.mapper.IdentifierMapper;
import at.tuwien.repository.sdb.IdentifierIdxRepository;
import at.tuwien.repository.mdb.IdentifierRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Component
@Log4j2
public class IndexConfig {

    private final IdentifierMapper identifierMapper;
    private final IdentifierRepository identifierRepository;
    private final IdentifierIdxRepository identifierIdxRepository;

    @Autowired
    public IndexConfig(IdentifierMapper identifierMapper, IdentifierRepository identifierRepository,
                       IdentifierIdxRepository identifierIdxRepository) {
        this.identifierMapper = identifierMapper;
        this.identifierRepository = identifierRepository;
        this.identifierIdxRepository = identifierIdxRepository;
    }

    @Transactional
    @EventListener(ApplicationReadyEvent.class)
    public void initIndex() {
        final List<IdentifierDto> identifiers = identifierRepository.findAll()
                .stream()
                .map(identifierMapper::identifierToIdentifierDto)
                .collect(Collectors.toList());
        identifierIdxRepository.saveAll(identifiers);
        log.info("Added {} identifiers to open search index", identifiers.size());
    }
}
