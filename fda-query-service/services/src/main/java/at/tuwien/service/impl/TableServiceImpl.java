package at.tuwien.service.impl;

import at.tuwien.entities.database.table.Table;
import at.tuwien.exception.DatabaseNotFoundException;
import at.tuwien.exception.TableNotFoundException;
import at.tuwien.repository.jpa.TableRepository;
import at.tuwien.service.TableService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Log4j2
@Service
public class TableServiceImpl implements TableService {

    private final TableRepository tableRepository;

    @Autowired
    public TableServiceImpl(TableRepository tableRepository) {
        this.tableRepository = tableRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Table find(Long databaseId, Long tableId) throws DatabaseNotFoundException, TableNotFoundException {
        final Optional<Table> table = tableRepository.findOne(databaseId, tableId);
        if (table.isEmpty()) {
            log.error("Failed to find table with id {} of database with id {} in metadata database", tableId,
                    databaseId);
            throw new TableNotFoundException("Failed to find table in metadata database");
        }
        return table.get();
    }
}
