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

import java.util.List;
import java.util.stream.Collectors;

@Component
@Log4j2
public class IndexInitializer {

    private final IdentifierMapper identifierMapper;
    private final IdentifierRepository identifierRepository;
    private final IdentifierIdxRepository identifierIdxRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    @Autowired
    public IndexInitializer(IdentifierMapper identifierMapper, IdentifierRepository identifierRepository,
                            IdentifierIdxRepository identifierIdxRepository,
                            ElasticsearchOperations elasticsearchOperations) {
        this.identifierMapper = identifierMapper;
        this.identifierRepository = identifierRepository;
        this.identifierIdxRepository = identifierIdxRepository;
        this.elasticsearchOperations = elasticsearchOperations;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initIndex() {
        log.debug("creating identifierindex");
        final IndexCoordinates identifierIndex = IndexCoordinates.of("identifierindex");
        if (!elasticsearchOperations.indexOps(identifierIndex).exists()) {
            elasticsearchOperations.indexOps(identifierIndex).create();
            elasticsearchOperations.indexOps(identifierIndex).createMapping(IdentifierDto.class);
        }
        final List<IdentifierDto> identifiers = identifierRepository.findAll()
                .stream()
                .map(identifierMapper::identifierToIdentifierDto)
                .collect(Collectors.toList());
        log.debug("add {} identifiers to elastic search index", identifiers.size());
        identifierIdxRepository.saveAll(identifiers);
    }
}
