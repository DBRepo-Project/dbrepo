package at.tuwien.service.impl;

import at.tuwien.api.semantics.EntityDto;
import at.tuwien.api.semantics.TableColumnEntityDto;
import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.database.table.columns.TableColumn;
import at.tuwien.entities.semantics.Ontology;
import at.tuwien.exception.DatabaseNotFoundException;
import at.tuwien.exception.QueryMalformedException;
import at.tuwien.exception.TableColumnNotFoundException;
import at.tuwien.exception.TableNotFoundException;
import at.tuwien.mapper.OntologyMapper;
import at.tuwien.repository.mdb.OntologyRepository;
import at.tuwien.repository.mdb.TableColumnRepository;
import at.tuwien.repository.mdb.TableRepository;
import at.tuwien.service.EntityService;
import at.tuwien.service.OntologyService;
import at.tuwien.service.QueryService;
import at.tuwien.service.TableService;
import lombok.extern.log4j.Log4j2;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.riot.RiotException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

@Log4j2
@Service
public class EntityServiceImpl implements EntityService {

    private final Dataset dataset;
    private final OntologyMapper ontologyMapper;
    private final OntologyRepository ontologyRepository;
    private final OntologyService ontologyService;
    private final TableService tableService;
    private final TableColumnRepository tableColumnRepository;

    @Autowired
    public EntityServiceImpl(OntologyRepository ontologyRepository, OntologyMapper ontologyMapper,
                             OntologyService ontologyService, TableService tableService,
                             TableColumnRepository tableColumnRepository) {
        this.ontologyMapper = ontologyMapper;
        this.ontologyRepository = ontologyRepository;
        this.dataset = DatasetFactory.create();
        this.ontologyService = ontologyService;
        this.tableService = tableService;
        this.tableColumnRepository = tableColumnRepository;
    }

    @Override
    public List<EntityDto> findByLabel(Ontology ontology, String label) throws QueryMalformedException {
        return findByLabel(ontology, label, 10);
    }

    @Override
    public List<EntityDto> findByLabel(Ontology ontology, String label, Integer limit) throws QueryMalformedException {
        final List<Ontology> ontologies = ontologyRepository.findAll();
        final String statement = ontologyMapper.ontologyToFindByLabelQuery(ontologies, ontology, label, limit);
        log.trace("execute sparql query:\n{}", statement);
        final List<EntityDto> results = new LinkedList<>();
        try (QueryExecution execution = QueryExecutionFactory.create(statement, this.dataset.getDefaultModel())) {
            final Iterator<QuerySolution> resultSet = execution.execSelect();
            while (resultSet.hasNext()) {
                final QuerySolution solution = resultSet.next();
                final RDFNode description = solution.get("description");
                final EntityDto entity = EntityDto.builder()
                        .uri(solution.get("o").toString())
                        .label(label)
                        .description(description != null ? description.asLiteral().getLexicalForm() : null)
                        .build();
                results.add(entity);
            }
        } catch (QueryParseException | IllegalArgumentException | RiotException e) {
            log.error("Failed to parse query: {}", e.getMessage());
            throw new QueryMalformedException("Failed to parse query: " + e.getMessage(), e);
        }
        return results;
    }

    @Override
    public List<EntityDto> findByUri(Ontology ontology, String uri) throws QueryMalformedException {
        final List<Ontology> ontologies = ontologyRepository.findAll();
        final String statement = ontologyMapper.ontologyToFindByUriQuery(ontologies, ontology, uri);
        log.trace("execute sparql query:\n{}", statement);
        try (QueryExecution execution = QueryExecutionFactory.create(statement, this.dataset.getDefaultModel())) {
            final Iterator<QuerySolution> resultSet = execution.execSelect();
            final List<EntityDto> results = new LinkedList<>();
            while (resultSet.hasNext()) {
                final QuerySolution solution = resultSet.next();
                final RDFNode label = solution.get("label");
                final RDFNode description = solution.get("description");
                final EntityDto entity = EntityDto.builder()
                        .uri(uri)
                        .label(label != null ? label.asLiteral().getLexicalForm() : null)
                        .description(description != null ? description.asLiteral().getLexicalForm() : null)
                        .build();
                results.add(entity);
            }
            return results;
        } catch (QueryParseException | IllegalArgumentException | RiotException e) {
            log.error("Failed to parse query: {}", e.getMessage());
            throw new QueryMalformedException("Failed to parse query: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<EntityDto> suggestTableSemantics(Long databaseId, Long tableId) throws TableNotFoundException,
            QueryMalformedException, DatabaseNotFoundException {
        final Table table = tableService.find(databaseId, tableId);
        final List<EntityDto> suggestions = new LinkedList<>();
        for (Ontology ontology : ontologyService.findAll()) {
            suggestions.addAll(findByLabel(ontology, table.getName(), 3));
        }
        log.debug("suggested {} semantic entities total", suggestions.size());
        return suggestions;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TableColumnEntityDto> suggestTableColumnSemantics(Long databaseId, Long tableId, Long columnId)
            throws QueryMalformedException, TableColumnNotFoundException {
        final Optional<TableColumn> optional = tableColumnRepository.findById(columnId);
        if (optional.isEmpty()) {
            log.error("Failed to find column with id {}", columnId);
            throw new TableColumnNotFoundException("Failed to find column with id " + columnId);
        }
        final List<TableColumnEntityDto> suggestions = new LinkedList<>();
        for (Ontology ontology : ontologyService.findAll()) {
            suggestions.addAll(findByLabel(ontology, optional.get().getName(), 3)
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
