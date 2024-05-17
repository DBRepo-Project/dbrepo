package at.tuwien.service.impl;

import at.tuwien.api.semantics.EntityDto;
import at.tuwien.api.semantics.TableColumnEntityDto;
import at.tuwien.config.JenaConfig;
import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.database.table.columns.TableColumn;
import at.tuwien.entities.semantics.Ontology;
import at.tuwien.exception.*;
import at.tuwien.mapper.OntologyMapper;
import at.tuwien.service.EntityService;
import at.tuwien.service.OntologyService;
import at.tuwien.service.TableService;
import lombok.extern.log4j.Log4j2;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RiotException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Log4j2
@Service
public class EntityServiceImpl implements EntityService {

    private final Dataset dataset;
    private final JenaConfig jenaConfig;
    private final OntologyMapper ontologyMapper;
    private final OntologyService ontologyService;

    @Autowired
    public EntityServiceImpl(Dataset dataset, JenaConfig jenaConfig, OntologyMapper ontologyMapper,
                             OntologyService ontologyService) {
        this.dataset = dataset;
        this.jenaConfig = jenaConfig;
        this.ontologyMapper = ontologyMapper;
        this.ontologyService = ontologyService;
    }

    public void validateOntology(Ontology ontology) throws MalformedException {
        if (ontology.getRdfPath() == null && ontology.getSparqlEndpoint() == null) {
            log.error("Ontology with uri {} is invalid: no RDF file present and no SPARQL endpoint found", ontology.getUri());
            throw new MalformedException("Ontology with uri " + ontology.getUri() + " is invalid: no RDF file present and no SPARQL endpoint found");
        }
    }

    @Override
    public List<EntityDto> findByLabel(Ontology ontology, String label) throws MalformedException {
        return findByLabel(ontology, label, 10);
    }

    @Override
    public List<EntityDto> findByLabel(Ontology ontology, String label, Integer limit) throws MalformedException {
        /* check */
        validateOntology(ontology);
        /* find */
        final List<Ontology> ontologies = ontologyService.findAll();
        final String statement = ontologyMapper.ontologyToFindByLabelQuery(ontologies, ontology, label, limit);
        log.trace("execute sparql query:\n{}", statement);
        final List<EntityDto> results = new LinkedList<>();
        if (ontology.getSparqlEndpoint() == null && ontology.getRdfPath() != null) {
            log.debug("load rdf model from path {}", ontology.getRdfPath());
            this.dataset.setDefaultModel(RDFDataMgr.loadModel(ontology.getRdfPath()));
        }
        try (QueryExecution execution = QueryExecutionDatasetBuilder.create()
                .model(this.dataset.getDefaultModel())
                .query(statement)
                .timeout(jenaConfig.getConnectionTimeout(), TimeUnit.MILLISECONDS)
                .build()) {
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
            throw new MalformedException("Failed to parse query: " + e.getMessage(), e);
        }
        return results;
    }

    @Override
    public List<EntityDto> findByUri(String uri) throws MalformedException, OntologyNotFoundException {
        /* find */
        final Ontology ontology = ontologyService.find(uri);
        final List<Ontology> ontologies = ontologyService.findAll();
        final String statement = ontologyMapper.ontologyToFindByUriQuery(ontologies, ontology, uri);
        log.trace("execute sparql query:\n{}", statement);
        try (QueryExecution execution = QueryExecutionDatasetBuilder.create()
                .model(this.dataset.getDefaultModel())
                .query(statement)
                .timeout(jenaConfig.getConnectionTimeout(), TimeUnit.MILLISECONDS)
                .build()) {
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
            throw new MalformedException("Failed to parse query: " + e.getMessage(), e);
        }
    }

    @Override
    public EntityDto findOneByUri(String uri) throws MalformedException, SemanticEntityNotFoundException,
            OntologyNotFoundException {
        /* find */
        final List<EntityDto> results = findByUri(uri);
        if (results.size() != 1) {
            log.error("None or multiple entities found for uri {}", uri);
            throw new SemanticEntityNotFoundException("None or multiple entities found for uri " + uri);
        }
        return results.get(0);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EntityDto> suggestByTable(Table table) throws MalformedException {
        final List<EntityDto> suggestions = new LinkedList<>();
        for (Ontology ontology : ontologyService.findAllProcessable()) {
            suggestions.addAll(findByLabel(ontology, table.getName(), 3));
        }
        return suggestions;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TableColumnEntityDto> suggestByColumn(TableColumn tableColumn) throws MalformedException {
        final List<TableColumnEntityDto> suggestions = new LinkedList<>();
        for (Ontology ontology : ontologyService.findAllProcessable()) {
            suggestions.addAll(findByLabel(ontology, tableColumn.getName(), 3)
                    .stream()
                    .map(e -> TableColumnEntityDto.builder()
                            .databaseId(tableColumn.getTable().getDatabase().getId())
                            .tableId(tableColumn.getTable().getId())
                            .columnId(tableColumn.getId())
                            .label(e.getLabel())
                            .uri(e.getUri())
                            .description(e.getDescription())
                            .build())
                    .toList());
        }
        return suggestions;
    }

}
