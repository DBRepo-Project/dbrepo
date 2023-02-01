package at.tuwien.config;

import at.tuwien.api.database.table.TableDto;
import at.tuwien.api.database.table.columns.ColumnDto;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.database.table.columns.TableColumn;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Component;

@Component
@Log4j2
public class IndexInitializer {

    private final Environment environment;
    private final ElasticsearchOperations elasticsearchOperations;

    @Autowired
    public IndexInitializer(Environment environment, ElasticsearchOperations elasticsearchOperations) {
        this.environment = environment;
        this.elasticsearchOperations = elasticsearchOperations;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initIndex() {
        if (environment.acceptsProfiles(Profiles.of("test-noelastic"))) {
            return;
        }
        log.debug("creating tableindex");
        final IndexCoordinates tableIndex = IndexCoordinates.of("tableindex");
        if (!elasticsearchOperations.indexOps(tableIndex).exists()) {
            elasticsearchOperations.indexOps(tableIndex).create();
            elasticsearchOperations.indexOps(tableIndex).createMapping(TableDto.class);
        }
        log.debug("creating columnindex");
        final IndexCoordinates columnIndex = IndexCoordinates.of("columnindex");
        if (!elasticsearchOperations.indexOps(columnIndex).exists()) {
            elasticsearchOperations.indexOps(columnIndex).create();
            elasticsearchOperations.indexOps(columnIndex).createMapping(ColumnDto.class);
        }
    }
}
