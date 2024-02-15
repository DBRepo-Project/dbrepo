package at.tuwien.service.impl;

import at.tuwien.api.database.table.columns.concepts.ColumnSemanticsUpdateDto;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.database.table.columns.TableColumn;
import at.tuwien.entities.database.table.columns.TableColumnConcept;
import at.tuwien.entities.database.table.columns.TableColumnUnit;
import at.tuwien.exception.*;
import at.tuwien.mapper.DatabaseMapper;
import at.tuwien.repository.mdb.DatabaseRepository;
import at.tuwien.repository.sdb.DatabaseIdxRepository;
import at.tuwien.service.SemanticService;
import at.tuwien.service.TableColumnService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Log4j2
@Service
public class TableColumnServiceImpl implements TableColumnService {

    private final DatabaseMapper databaseMapper;
    private final SemanticService semanticService;
    private final DatabaseRepository databaseRepository;
    private final DatabaseIdxRepository databaseIdxRepository;

    @Autowired
    public TableColumnServiceImpl(DatabaseMapper databaseMapper, SemanticService semanticService,
                                  DatabaseRepository databaseRepository, DatabaseIdxRepository databaseIdxRepository) {
        this.databaseMapper = databaseMapper;
        this.semanticService = semanticService;
        this.databaseRepository = databaseRepository;
        this.databaseIdxRepository = databaseIdxRepository;
    }

    @Transactional(readOnly = true)
    public Database find(Long databaseId) throws DatabaseNotFoundException {
        final Optional<Database> database = databaseRepository.findById(databaseId);
        if (database.isEmpty()) {
            log.error("Failed to find database with id {} in metadata database", databaseId);
            throw new DatabaseNotFoundException("could not find database with id " + databaseId + " in metadata database");
        }
        return database.get();
    }

    @Transactional(readOnly = true)
    public Table find(Long databaseId, Long tableId) throws DatabaseNotFoundException, TableNotFoundException {
        final Optional<Table> table = find(databaseId)
                .getTables()
                .stream()
                .filter(t -> t.getId().equals(tableId))
                .findFirst();
        if (table.isEmpty()) {
            log.error("Failed to find table with id {} in metadata database", tableId);
            throw new TableNotFoundException("Failed to find table with id " + tableId + " in metadata database");
        }
        return table.get();
    }

    @Override
    @Transactional
    public TableColumn update(Long databaseId, Long tableId, Long columnId, ColumnSemanticsUpdateDto updateDto)
            throws TableNotFoundException, DatabaseNotFoundException, TableMalformedException {
        final Table table = find(databaseId, tableId);
        final TableColumn column = findColumn(table, columnId);
        /* assign */
        if (updateDto.getUnitUri() != null) {
            try {
                column.setUnit(semanticService.findUnit(updateDto.getUnitUri()));
                log.debug("found unit with uri {} in metadata database", updateDto.getUnitUri());
            } catch (UnitNotFoundException e) {
                final TableColumnUnit unit = TableColumnUnit.builder()
                        .uri(updateDto.getUnitUri())
                        .build();
                column.setUnit(unit);
            }
        } else {
            column.setUnit(null);
        }
        if (updateDto.getConceptUri() != null) {
            try {
                column.setConcept(semanticService.findConcept(updateDto.getConceptUri()));
                log.debug("found concept with uri {} in metadata database", updateDto.getConceptUri());
            } catch (ConceptNotFoundException e) {
                final TableColumnConcept concept = TableColumnConcept.builder()
                        .uri(updateDto.getConceptUri())
                        .build();
                column.setConcept(concept);
            }
        } else {
            column.setConcept(null);
        }
        /* update in metadata database */
        table.getColumns().set(table.getColumns().indexOf(column), column);
        databaseRepository.save(table.getDatabase());
        /* update in open search database */
        databaseIdxRepository.save(databaseMapper.databaseToDatabaseDto(find(databaseId)));
        log.info("Updated table column with id {} of table with id {} in metadata database & search database", columnId, tableId);
        return column;
    }

    @Override
    @Transactional(readOnly = true)
    public TableColumn findColumn(Table table, Long columnId) throws TableMalformedException {
        final Optional<TableColumn> optional = table.getColumns()
                .stream()
                .filter(c -> c.getId().equals(columnId))
                .findFirst();
        if (optional.isEmpty()) {
            log.error("Failed to find column with id {} in metadata database", columnId);
            throw new TableMalformedException("Failed to find column with id " + columnId + "  in metadata database");
        }
        return optional.get();
    }

    @Override
    @Transactional(readOnly = true)
    public TableColumn findColumn(Table table, String name) throws TableMalformedException {
        final Optional<TableColumn> optional = table.getColumns()
                .stream()
                .filter(c -> c.getInternalName().equals(name))
                .findFirst();
        if (optional.isEmpty()) {
            log.error("Failed to find column with name {} in table with name {}", name, table.getInternalName());
            throw new TableMalformedException("Failed to find column with name " + name + "  in table with name " + table.getInternalName());
        }
        return optional.get();
    }

    @Override
    @Transactional(readOnly = true)
    public TableColumn findColumn(Database database, String tableName, String columnName)
            throws TableMalformedException {
        final Optional<TableColumn> optional = database.getTables()
                .stream()
                .filter(t -> t.getInternalName().equals(tableName))
                .map(Table::getColumns)
                .flatMap(List::stream)
                .filter(c -> c.getInternalName().equals(columnName))
                .findFirst();
        if (optional.isEmpty()) {
            log.error("Failed to find column {}.{} in database with id {}", tableName, columnName, database.getId());
            throw new TableMalformedException("Failed to find column " + tableName + "." + columnName + " in database with id " + database.getId());
        }
        return optional.get();
    }
}
