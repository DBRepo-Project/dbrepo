package at.tuwien.service.impl;

import at.tuwien.api.semantics.EntityDto;
import at.tuwien.entities.semantics.Ontology;
import at.tuwien.exception.QueryMalformedException;
import at.tuwien.repository.jpa.OntologyRepository;
import at.tuwien.service.QueryService;
import lombok.extern.log4j.Log4j2;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.*;
import org.apache.jena.riot.RiotException;
import org.apache.jena.sparql.algebra.op.OpService;
import org.apache.jena.sparql.service.ServiceExecutorRegistry;
import org.apache.jena.sparql.service.single.ChainingServiceExecutor;
import org.apache.jena.sparql.util.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

@Log4j2
@Service("sparqlService")
public class SparqlServiceImpl implements QueryService {

    private final Dataset dataset;

    @Autowired
    public SparqlServiceImpl(OntologyRepository ontologyRepository) {
        final Context context = ARQ.getContext().copy();
        this.dataset = DatasetFactory.create();
        /* registry */
        final ServiceExecutorRegistry registry = ServiceExecutorRegistry.get(context).copy();
        ontologyRepository.findAll()
                .stream()
                .filter(o -> Objects.nonNull(o.getSparqlEndpoint()))
                .forEach(ontology -> {
                    final Node node = NodeFactory.createURI(ontology.getSparqlEndpoint());
                    ChainingServiceExecutor relaySef = (opExecute, original, binding, execCxt, chain) -> {
                        if (opExecute.getService().equals(node)) {
                            opExecute = new OpService(node, opExecute.getSubOp(), opExecute.getSilent());
                        }
                        return chain.createExecution(opExecute, original, binding, execCxt);
                    };
                    log.debug("add sparql endpoint {}", ontology.getSparqlEndpoint());
                    registry.addSingleLink(relaySef);
                });
        ServiceExecutorRegistry.set(context, registry);
    }

    @Override
    public List<EntityDto> findByLabel(Ontology ontology, String label) throws QueryMalformedException {
        return findByLabel(ontology, label, 10);
    }

    @Override
    public List<EntityDto> findByLabel(Ontology ontology, String label, Integer limit) throws QueryMalformedException {
        final String statement = String.join("\n",
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>",
                "SELECT * {",
                "  SERVICE <" + ontology.getSparqlEndpoint() + "> {",
                "    SELECT ?o ?label {",
                "      ?o rdfs:label \"" + label.replace("\"", "") + "\"@en .",
                "      ?o rdfs:label ?label .",
                "      FILTER (langMatches(lang(?label), \"EN\" ) )",
                "     } LIMIT " + limit,
                "  }",
                "}");
        log.trace("compiled remote query {}", statement);
        final List<EntityDto> results = new LinkedList<>();
        try (QueryExecution execution = QueryExecutionFactory.create(statement, this.dataset.getDefaultModel())) {
            final Iterator<QuerySolution> resultSet = execution.execSelect();
            while (resultSet.hasNext()) {
                final QuerySolution solution = resultSet.next();
                final EntityDto entity = EntityDto.builder()
                        .uri(solution.get("o").toString())
                        .label(solution.get("label").asLiteral().getLexicalForm())
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
    public EntityDto findByUri(Ontology ontology, String uri) throws QueryMalformedException {
        final String statement = String.join("\n",
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>",
                "SELECT * {",
                "  SERVICE <" + ontology.getSparqlEndpoint() + "> {",
                "    SELECT ?label {",
                "      <" + uri + "> rdfs:label ?label .",
                "      FILTER (langMatches(lang(?label), \"EN\" ) )",
                "     } LIMIT 1",
                "  }",
                "}");
        log.trace("compiled remote query {}", statement);
        try (QueryExecution execution = QueryExecutionFactory.create(statement, this.dataset.getDefaultModel())) {
            final Iterator<QuerySolution> resultSet = execution.execSelect();
            while (resultSet.hasNext()) {
                final QuerySolution solution = resultSet.next();
                final EntityDto entity = EntityDto.builder()
                        .uri(solution.get("o").toString())
                        .label(solution.get("label").asLiteral().getLexicalForm())
                        .build();
                return entity;
            }
        } catch (QueryParseException | IllegalArgumentException | RiotException e) {
            log.error("Failed to parse query: {}", e.getMessage());
            throw new QueryMalformedException("Failed to parse query: " + e.getMessage(), e);
        }
        log.error("Failed to find label: did not produce a result for uri {}", uri);
        throw new QueryMalformedException("Failed to find uri: did not produce a result for uri " + uri);
    }

}
