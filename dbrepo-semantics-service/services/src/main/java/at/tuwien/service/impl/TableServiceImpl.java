package at.tuwien.service.impl;

import at.tuwien.api.semantics.EntitySearchDto;
import at.tuwien.api.semantics.TableColumnEntityDto;
import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.database.table.columns.TableColumn;
import at.tuwien.entities.semantics.Ontology;
import at.tuwien.exception.QueryMalformedException;
import at.tuwien.exception.TableNotFoundException;
import at.tuwien.repository.jpa.TableRepository;
import at.tuwien.service.OntologyService;
import at.tuwien.service.QueryService;
import at.tuwien.service.TableService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

@Log4j2
@Service
public class TableServiceImpl implements TableService {

    private final OntologyService ontologyService;
    private final TableRepository tableRepository;
    private final QueryService sparqlService;
    private final QueryService rdfService;

    @Autowired
    public TableServiceImpl(OntologyService ontologyService, TableRepository tableRepository,
                            @Qualifier("sparqlService") QueryService sparqlService,
                            @Qualifier("rdfService") QueryService rdfService) {
        this.ontologyService = ontologyService;
        this.tableRepository = tableRepository;
        this.sparqlService = sparqlService;
        this.rdfService = rdfService;
    }

    @Override
    public Table find(Long databaseId, Long tableId) throws TableNotFoundException {
        final Optional<Table> optional = tableRepository.findByDatabaseIdAndId(databaseId, tableId);
        if (optional.isEmpty()) {
            log.error("Failed to find table with id {} in database with id {}", tableId, databaseId);
            throw new TableNotFoundException("Failed to find table with id " + tableId + " in database with id " + databaseId);
        }
        return optional.get();
    }

    @Override
    public List<TableColumnEntityDto> suggest(Long databaseId, Long tableId) throws TableNotFoundException, QueryMalformedException {
        final Table table = find(databaseId, tableId);
        final List<TableColumnEntityDto> suggestions = new LinkedList<>();
        for (TableColumn column : table.getColumns()) {
            for (Ontology ontology : ontologyService.findAll()) {
                final QueryService service = ontology.getSparqlEndpoint() != null ? sparqlService : rdfService;
                suggestions.addAll(service.findByLabel(ontology, column.getName(), 3)
                        .stream()
                        .map(e -> TableColumnEntityDto.builder()
                                .databaseId(databaseId)
                                .tableId(tableId)
                                .columnId(column.getId())
                                .label(e.getLabel())
                                .uri(e.getUri())
                                .build())
                        .toList());
            }
        }
        log.debug("suggested {} semantic entities total", suggestions.size());
        return suggestions;
    }

}
