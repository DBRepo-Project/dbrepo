package at.tuwien.config;

import at.tuwien.api.database.table.TableDto;
import at.tuwien.api.database.table.columns.ColumnDto;
import at.tuwien.mapper.TableMapper;
import at.tuwien.repository.elastic.TableColumnIdxRepository;
import at.tuwien.repository.elastic.TableIdxRepository;
import at.tuwien.repository.jpa.TableColumnRepository;
import at.tuwien.repository.jpa.TableRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Component
@Log4j2
public class IndexConfig {

    private final Environment environment;
    private final TableMapper tableMapper;
    private final TableRepository tableRepository;
    private final TableIdxRepository tableIdxRepository;
    private final TableColumnRepository tableColumnRepository;
    private final ElasticsearchOperations elasticsearchOperations;
    private final TableColumnIdxRepository tableColumnIdxRepository;

    @Autowired
    public IndexConfig(Environment environment, TableMapper tableMapper, TableRepository tableRepository,
                       TableIdxRepository tableIdxRepository, TableColumnRepository tableColumnRepository,
                       ElasticsearchOperations elasticsearchOperations,
                       TableColumnIdxRepository tableColumnIdxRepository) {
        this.environment = environment;
        this.tableMapper = tableMapper;
        this.tableRepository = tableRepository;
        this.tableIdxRepository = tableIdxRepository;
        this.tableColumnRepository = tableColumnRepository;
        this.elasticsearchOperations = elasticsearchOperations;
        this.tableColumnIdxRepository = tableColumnIdxRepository;
    }

    @Transactional
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
        /* pre-fill */
        final List<TableDto> tables = tableRepository.findAll()
                .stream()
                .map(tableMapper::tableToTableDto)
                .collect(Collectors.toList());
        log.debug("add {} tables to elastic search index", tables.size());
        tableIdxRepository.saveAll(tables);
        final List<ColumnDto> columns = tableColumnRepository.findAll()
                .stream()
                .map(tableMapper::tableColumnToColumnDto)
                .collect(Collectors.toList());
        log.debug("add {} columns to elastic search index", columns.size());
        tableColumnIdxRepository.saveAll(columns);
    }
}
