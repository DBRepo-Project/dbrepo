package at.tuwien.service.impl;

import at.tuwien.api.semantics.EntityDto;
import at.tuwien.entities.semantics.Ontology;
import at.tuwien.exception.QueryMalformedException;
import at.tuwien.mapper.OntologyMapper;
import at.tuwien.repository.mdb.OntologyRepository;
import at.tuwien.service.QueryService;
import lombok.extern.log4j.Log4j2;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.riot.RiotException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

@Log4j2
@Service
public class QueryServiceImpl implements QueryService {

    private final Dataset dataset;
    private final OntologyMapper ontologyMapper;
    private final OntologyRepository ontologyRepository;

    @Autowired
    public QueryServiceImpl(OntologyRepository ontologyRepository, OntologyMapper ontologyMapper) {
        this.ontologyMapper = ontologyMapper;
        this.ontologyRepository = ontologyRepository;
        this.dataset = DatasetFactory.create();
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

}
