package at.tuwien.mapper;

import at.tuwien.api.database.table.columns.concepts.ConceptDto;
import at.tuwien.api.database.table.columns.concepts.ConceptSaveDto;
import at.tuwien.api.database.table.columns.concepts.UnitDto;
import at.tuwien.api.database.table.columns.concepts.UnitSaveDto;
import at.tuwien.api.semantics.OntologyBriefDto;
import at.tuwien.api.semantics.OntologyCreateDto;
import at.tuwien.api.semantics.OntologyDto;
import at.tuwien.entities.database.table.columns.TableColumnConcept;
import at.tuwien.entities.database.table.columns.TableColumnUnit;
import at.tuwien.entities.semantics.Ontology;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;


@Mapper(componentModel = "spring")
public interface OntologyMapper {

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OntologyMapper.class);

    @Mappings({
            @Mapping(target = "rdf", expression = "java(true)"),
            @Mapping(target = "sparql", expression = "java(data.getSparqlEndpoint() != null)")
    })
    OntologyDto ontologyToOntologyDto(Ontology data);

    @Mappings({
            @Mapping(target = "rdf", expression = "java(true)"),
            @Mapping(target = "sparql", expression = "java(data.getSparqlEndpoint() != null)")
    })
    OntologyBriefDto ontologyToOntologyBriefDto(Ontology data);

    Ontology ontologyCreateDtoToOntology(OntologyCreateDto data);

    ConceptDto tableColumnConceptToConceptDto(TableColumnConcept data);

    UnitDto tableColumnUnitToUnitDto(TableColumnUnit data);

    TableColumnUnit unitSaveDtoToTableColumnUnit(UnitSaveDto data);

    TableColumnConcept conceptSaveDtoToTableColumnConcept(ConceptSaveDto data);

    default String ontologyToFindByLabelQuery(Ontology ontology, String label, Integer limit) {
        if (ontology.getSparqlEndpoint() != null) {
            /* prefer SPARQL endpoint over rdf */
            return String.join("\n",
                    "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>",
                    "SELECT * {",
                    "  SERVICE <" + ontology.getSparqlEndpoint() + "> {",
                    "    SELECT ?o ?label ?comment {",
                    "      ?o rdfs:label \"" + label.replace("\"", "") + "\"@en .",
                    "      ?o rdfs:label ?label .",
                    "      OPTIONAL {?o rdfs:comment ?comment} .",
                    "      FILTER (langMatches(lang(?label), \"EN\" ) )",
                    "      FILTER (langMatches(lang(?comment), \"EN\" ) )",
                    "     } LIMIT " + limit,
                    "  }",
                    "}");
        }
        return String.join("\n",
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>",
                "SELECT ?o ?label ?comment {",
                "  ?o rdfs:label \"" + label.replace("\"", "") + "\"@en .",
                "  ?o rdfs:label ?label .",
                "  OPTIONAL {?o rdfs:comment ?comment} .",
                "  FILTER (langMatches(lang(?label), \"EN\" ) )",
                "  FILTER (langMatches(lang(?comment), \"EN\" ) )",
                "} LIMIT " + limit);
    }

    default String ontologyToFindByUriQuery(Ontology ontology, String uri) {
        if (ontology.getSparqlEndpoint() != null) {
            /* prefer SPARQL endpoint over rdf */
            return String.join("\n",
                    "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>",
                    "SELECT * {",
                    "  SERVICE <" + ontology.getSparqlEndpoint() + "> {",
                    "    SELECT ?label ?comment {",
                    "      <" + uri + "> rdfs:label ?label .",
                    "      OPTIONAL {<" + uri + "> rdfs:comment ?comment} .",
                    "      FILTER (langMatches(lang(?label), \"EN\" ) )",
                    "      FILTER (langMatches(lang(?comment), \"EN\" ) )",
                    "     } LIMIT 1",
                    "  }",
                    "}");
        }
        return String.join("\n",
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>",
                "SELECT ?label ?comment {",
                "  <" + uri + "> rdfs:label ?label .",
                "  OPTIONAL {<" + uri + "> rdfs:comment ?comment} .",
                "  FILTER (langMatches(lang(?label), \"EN\" ) )",
                "  FILTER (langMatches(lang(?comment), \"EN\" ) )",
                "} LIMIT 1");
    }

}
