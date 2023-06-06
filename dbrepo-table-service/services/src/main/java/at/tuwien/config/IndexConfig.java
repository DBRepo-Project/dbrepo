package at.tuwien.config;

import at.tuwien.api.database.table.TableDto;
import at.tuwien.api.database.table.columns.ColumnDto;
import at.tuwien.mapper.TableMapper;
import at.tuwien.repository.mdb.TableColumnRepository;
import at.tuwien.repository.mdb.TableRepository;
import at.tuwien.repository.sdb.TableColumnIdxRepository;
import at.tuwien.repository.sdb.TableIdxRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Component
@Log4j2
public class IndexConfig {

    private final TableMapper tableMapper;
    private final TableRepository tableRepository;
    private final TableIdxRepository tableIdxRepository;
    private final TableColumnRepository tableColumnRepository;
    private final TableColumnIdxRepository tableColumnIdxRepository;

    public IndexConfig(TableMapper tableMapper, TableRepository tableRepository, TableIdxRepository tableIdxRepository,
                       TableColumnRepository tableColumnRepository, TableColumnIdxRepository tableColumnIdxRepository) {
        this.tableMapper = tableMapper;
        this.tableRepository = tableRepository;
        this.tableIdxRepository = tableIdxRepository;
        this.tableColumnRepository = tableColumnRepository;
        this.tableColumnIdxRepository = tableColumnIdxRepository;
    }

    @Transactional
    @EventListener(ApplicationReadyEvent.class)
    public void initIndex() {
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
