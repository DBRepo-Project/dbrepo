package at.tuwien.config;

import at.tuwien.api.database.DatabaseDto;
import at.tuwien.mapper.DatabaseMapper;
import at.tuwien.repository.elastic.DatabaseIdxRepository;
import at.tuwien.repository.jpa.DatabaseRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@Component
public class IndexConfig {

    private final Environment environment;
    private final DatabaseMapper databaseMapper;
    private final DatabaseRepository databaseRepository;
    private final DatabaseIdxRepository databaseIdxRepository;
    private final ElasticsearchOperations elasticsearchOperations;
    private final ApplicationEventPublisher applicationEventPublisher;

    public IndexConfig(Environment environment, DatabaseMapper databaseMapper, DatabaseRepository databaseRepository,
                       DatabaseIdxRepository databaseIdxRepository, ElasticsearchOperations elasticsearchOperations,
                       ApplicationEventPublisher applicationEventPublisher) {
        this.environment = environment;
        this.databaseMapper = databaseMapper;
        this.databaseRepository = databaseRepository;
        this.databaseIdxRepository = databaseIdxRepository;
        this.elasticsearchOperations = elasticsearchOperations;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initIndex() {
        log.debug("creating databaseindex");
        final IndexCoordinates databaseIndex = IndexCoordinates.of("databaseindex");
        if (!elasticsearchOperations.indexOps(databaseIndex).exists()) {
            elasticsearchOperations.indexOps(databaseIndex).create();
            elasticsearchOperations.indexOps(databaseIndex).createMapping(DatabaseDto.class);
        }
        final List<DatabaseDto> databases = databaseRepository.findAll()
                .stream()
                .map(databaseMapper::databaseToDatabaseDto)
                .collect(Collectors.toList());
        log.debug("add {} databases to elastic search index", databases.size());
        databaseIdxRepository.saveAll(databases);
    }

}
