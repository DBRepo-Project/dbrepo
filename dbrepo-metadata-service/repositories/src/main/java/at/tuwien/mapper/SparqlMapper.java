package at.tuwien.mapper;

import at.tuwien.entities.semantics.Ontology;
import org.mapstruct.Mapper;

import java.util.List;


@Mapper(componentModel = "spring")
public interface SparqlMapper {

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SparqlMapper.class);

    default String defaultNamespaces(List<Ontology> data) {
        return String.join("\n",
                data.stream()
                        .map(o -> "PREFIX " + o.getPrefix() + ": <" + o.getUri() + ">")
                        .toList());
    }

    default String ontologyToFindByLabelQuery(List<Ontology> ontologies, Ontology ontology, String label, Integer limit) {
        if (ontology.getSparqlEndpoint() != null) {
            /* prefer SPARQL endpoint over rdf */
            return String.join("\n",
                    defaultNamespaces(ontologies),
                    "SELECT * {",
                    "  SERVICE <" + ontology.getSparqlEndpoint() + "> {",
                    "    SELECT ?o ?label ?description {",
                    "      ?o rdfs:label \"" + label.replace("\"", "") + "\"@en .",
                    "      ?o rdfs:label ?label .",
                    "      FILTER (LANG(?label) = 'en')",
                    "      OPTIONAL {",
                    "        ?o schema:description ?description",
                    "        FILTER (LANG(?description) = 'en')",
                    "      }",
                    "    } LIMIT " + limit,
                    "  }",
                    "}");
        }
        return String.join("\n",
                defaultNamespaces(ontologies),
                "SELECT ?o ?label ?comment {",
                "  ?o rdfs:label \"" + label.replace("\"", "") + "\"@en .",
                "  OPTIONAL {",
                "    ?o rdfs:label ?label",
                "    FILTER (LANG(?label) = 'en')",
                "  }",
                "  OPTIONAL {",
                "    ?o schema:description ?description",
                "    FILTER (LANG(?description) = 'en')",
                "  }",
                "} LIMIT " + limit);
    }

    default String ontologyToFindByUriQuery(List<Ontology> ontologies, Ontology ontology, String uri) {
        if (ontology.getSparqlEndpoint() != null) {
            /* prefer SPARQL endpoint over rdf */
            return String.join("\n",
                    defaultNamespaces(ontologies),
                    "SELECT * {",
                    "  SERVICE <" + ontology.getSparqlEndpoint() + "> {",
                    "    SELECT ?label ?description {",
                    "      OPTIONAL {",
                    "        <" + uri + "> rdfs:label ?label",
                    "        FILTER (LANG(?label) = 'en')",
                    "      }",
                    "      OPTIONAL {",
                    "        <" + uri + "> schema:description ?description",
                    "        FILTER (LANG(?description) = 'en')",
                    "      }",
                    "     } LIMIT 1",
                    "  }",
                    "}");
        }
        return String.join("\n",
                defaultNamespaces(ontologies),
                "SELECT ?label ?description {",
                "  OPTIONAL {",
                "    <" + uri + "> rdfs:label ?label",
                "    FILTER (LANG(?label) = 'en')",
                "  }",
                "  OPTIONAL {",
                "    <" + uri + "> schema:description ?description",
                "    FILTER (LANG(?description) = 'en')",
                "  }",
                "} LIMIT 1");
    }

}
