package at.tuwien.service.impl;

import at.tuwien.api.semantics.EntityDto;
import at.tuwien.api.semantics.EntitySearchDto;
import at.tuwien.entities.semantics.Ontology;
import at.tuwien.exception.QueryMalformedException;
import at.tuwien.service.QueryService;
import lombok.extern.log4j.Log4j2;
import org.apache.jena.query.*;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.riot.RiotException;
import org.apache.jena.shared.JenaException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

@Log4j2
@Service("rdfService")
public class RdfServiceImpl implements QueryService {

    private final Dataset dataset;

    @Autowired
    public RdfServiceImpl() {
        this.dataset = DatasetFactory.create();
    }

    @Override
    public List<EntityDto> findByLabel(Ontology ontology, String label) throws QueryMalformedException {
        return findByLabel(ontology, label, 10);
    }

    @Override
    public List<EntityDto> findByLabel(Ontology ontology, String label, Integer limit) throws QueryMalformedException {
        final String statement = String.join("\n",
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>",
                "SELECT ?o ?label {",
                "  ?o rdfs:label \"" + label.replace("\"", "") + "\"@en .",
                "  ?o rdfs:label ?label .",
                "  FILTER (langMatches(lang(?label), \"EN\" ) )",
                "} LIMIT " + limit);
        log.trace("compiled local query {}", statement);
        final RDFConnection conn = RDFConnection.connect(this.dataset);
        conn.load(ontology.getLocal());
        final List<EntityDto> results = new LinkedList<>();
        try {
            conn.querySelect(statement, (qs) -> {
                final EntityDto entity = EntityDto.builder()
                        .uri(qs.getResource("o").toString())
                        .label(qs.getLiteral("label").getLexicalForm())
                        .build();
                results.add(entity);
            });
            conn.close();
            return results;
        } catch (JenaException e) {
            log.error("Failed to parse query: {}", e.getMessage());
            throw new QueryMalformedException("Failed to parse query: " + e.getMessage(), e);
        }
    }

    @Override
    public EntityDto findByUri(Ontology ontology, String uri) throws QueryMalformedException {
        final String statement = String.join("\n",
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>",
                "SELECT ?label {",
                "  <" + uri + "> rdfs:label ?label .",
                "  FILTER (langMatches(lang(?label), \"EN\" ) )",
                "} LIMIT 1");
        log.trace("compiled local query {}", statement);
        final RDFConnection conn = RDFConnection.connect(this.dataset);
        conn.load(ontology.getLocal());
        final List<EntityDto> results = new LinkedList<>();
        try {
            conn.querySelect(statement, (qs) -> {
                final EntityDto entity = EntityDto.builder()
                        .uri(qs.getResource("o").toString())
                        .label(qs.getLiteral("label").getLexicalForm())
                        .build();
                results.add(entity);
            });
            conn.close();
        } catch (JenaException e) {
            log.error("Failed to parse query: {}", e.getMessage());
            throw new QueryMalformedException("Failed to parse query: " + e.getMessage(), e);
        }
        if (results.size() != 1) {
            log.error("Failed to find label: did not produce a result for uri {}", uri);
            throw new QueryMalformedException("Failed to find uri: did not produce a result for uri " + uri);
        }
        return results.get(0);
    }
}
