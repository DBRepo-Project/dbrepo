package at.tuwien.config;

import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.database.table.columns.TableColumn;
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

@Component
@Log4j2
public class IndexConfig {

    private final TableRepository tableRepository;
    private final TableIdxRepository tableIdxRepository;
    private final TableColumnRepository tableColumnRepository;
    private final TableColumnIdxRepository tableColumnIdxRepository;

    public IndexConfig(TableRepository tableRepository, TableIdxRepository tableIdxRepository,
                       TableColumnRepository tableColumnRepository, TableColumnIdxRepository tableColumnIdxRepository) {
        this.tableRepository = tableRepository;
        this.tableIdxRepository = tableIdxRepository;
        this.tableColumnRepository = tableColumnRepository;
        this.tableColumnIdxRepository = tableColumnIdxRepository;
    }

    @Transactional
    @EventListener(ApplicationReadyEvent.class)
    public void initIndex() {
        final List<Table> tables = tableRepository.findAll();
        tableIdxRepository.saveAll(tables);
        log.info("Added {} tables to open search index", tables.size());
        final List<TableColumn> columns = tableColumnRepository.findAll();
        tableColumnIdxRepository.saveAll(columns);
        log.info("Added {} columns to open search index", columns.size());
    }
}
