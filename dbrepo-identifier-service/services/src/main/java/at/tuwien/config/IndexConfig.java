package at.tuwien.config;

import at.tuwien.api.identifier.IdentifierDto;
import at.tuwien.mapper.IdentifierMapper;
import at.tuwien.repository.elastic.IdentifierIdxRepository;
import at.tuwien.repository.jpa.IdentifierRepository;
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
    private final ElasticsearchOperations elasticsearchOperations;

    @Autowired
    public IndexConfig(IdentifierMapper identifierMapper, IdentifierRepository identifierRepository,
                       IdentifierIdxRepository identifierIdxRepository,
                       ElasticsearchOperations elasticsearchOperations) {
        this.identifierMapper = identifierMapper;
        this.identifierRepository = identifierRepository;
        this.identifierIdxRepository = identifierIdxRepository;
        this.elasticsearchOperations = elasticsearchOperations;
    }

    @Transactional
    @EventListener(ApplicationReadyEvent.class)
    public void initIndex() {
        final IndexCoordinates identifierIndex = IndexCoordinates.of("identifier");
        if (!elasticsearchOperations.indexOps(identifierIndex).exists()) {
            elasticsearchOperations.indexOps(identifierIndex).create();
            elasticsearchOperations.indexOps(identifierIndex).createMapping(IdentifierDto.class);
            log.info("Created identifier index");
        }
        final List<IdentifierDto> identifiers = identifierRepository.findAll()
                .stream()
                .map(identifierMapper::identifierToIdentifierDto)
                .collect(Collectors.toList());
        log.info("Add {} identifiers to OpenSearch index", identifiers.size());
        identifierIdxRepository.saveAll(identifiers);
    }
}
