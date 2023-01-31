package at.tuwien.config;

import at.tuwien.entities.database.Database;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Component;

@Component
@Log4j2
public class IndexInitializer {

    private final ElasticsearchOperations elasticsearchOperations;

    public IndexInitializer(ElasticsearchOperations elasticsearchOperations) {
        this.elasticsearchOperations = elasticsearchOperations;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initIndex() {
        log.debug("creating databaseindex");
        final IndexCoordinates databaseIndex = IndexCoordinates.of("databaseindex");
        if (!elasticsearchOperations.indexOps(databaseIndex).exists()) {
            elasticsearchOperations.indexOps(databaseIndex).create();
            elasticsearchOperations.indexOps(databaseIndex).createMapping(Database.class);
        }
    }
}
