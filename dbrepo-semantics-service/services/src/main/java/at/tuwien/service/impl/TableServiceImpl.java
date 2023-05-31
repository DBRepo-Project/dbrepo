package at.tuwien.service.impl;

import at.tuwien.api.semantics.EntityDto;
import at.tuwien.api.semantics.TableColumnEntityDto;
import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.database.table.columns.TableColumn;
import at.tuwien.entities.database.table.columns.TableColumnKey;
import at.tuwien.entities.semantics.Ontology;
import at.tuwien.exception.QueryMalformedException;
import at.tuwien.exception.TableColumnNotFoundException;
import at.tuwien.exception.TableNotFoundException;
import at.tuwien.mapper.TableMapper;
import at.tuwien.repository.jpa.TableColumnRepository;
import at.tuwien.repository.jpa.TableRepository;
import at.tuwien.service.OntologyService;
import at.tuwien.service.QueryService;
import at.tuwien.service.TableService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

@Log4j2
@Service
public class TableServiceImpl implements TableService {

    private final TableMapper tableMapper;
    private final QueryService queryService;
    private final OntologyService ontologyService;
    private final TableRepository tableRepository;
    private final TableColumnRepository tableColumnRepository;

    @Autowired
    public TableServiceImpl(TableMapper tableMapper, OntologyService ontologyService, TableRepository tableRepository,
                            QueryService queryService, TableColumnRepository tableColumnRepository) {
        this.tableMapper = tableMapper;
        this.queryService = queryService;
        this.ontologyService = ontologyService;
        this.tableRepository = tableRepository;
        this.tableColumnRepository = tableColumnRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Table find(Long databaseId, Long tableId) throws TableNotFoundException {
        final Optional<Table> optional = tableRepository.findByDatabaseIdAndId(databaseId, tableId);
        if (optional.isEmpty()) {
            log.error("Failed to find table with id {} in database with id {}", tableId, databaseId);
            throw new TableNotFoundException("Failed to find table with id " + tableId + " in database with id " + databaseId);
        }
        return optional.get();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EntityDto> suggestTableSemantics(Long databaseId, Long tableId) throws TableNotFoundException,
            QueryMalformedException {
        final Table table = find(databaseId, tableId);
        final List<EntityDto> suggestions = new LinkedList<>();
        for (Ontology ontology : ontologyService.findAll()) {
            suggestions.addAll(queryService.findByLabel(ontology, table.getName(), 3));
        }
        log.debug("suggested {} semantic entities total", suggestions.size());
        return suggestions;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TableColumnEntityDto> suggestTableColumnSemantics(Long databaseId, Long tableId, Long columnId)
            throws QueryMalformedException, TableColumnNotFoundException {
        final TableColumnKey key = tableMapper.toTableColumnKey(databaseId, tableId, columnId);
        final Optional<TableColumn> optional = tableColumnRepository.findById(key);
        if (optional.isEmpty()) {
            log.error("Failed to find column with key {}", key);
            throw new TableColumnNotFoundException("Failed to find column with key " + key);
        }
        final List<TableColumnEntityDto> suggestions = new LinkedList<>();
        for (Ontology ontology : ontologyService.findAll()) {
            suggestions.addAll(queryService.findByLabel(ontology, optional.get().getName(), 3)
                    .stream()
                    .map(e -> TableColumnEntityDto.builder()
                            .databaseId(databaseId)
                            .tableId(tableId)
                            .columnId(optional.get().getId())
                            .label(e.getLabel())
                            .uri(e.getUri())
                            .description(e.getDescription())
                            .build())
                    .toList());
        }
        log.debug("suggested {} semantic entities total", suggestions.size());
        return suggestions;
    }

}
