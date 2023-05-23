package at.tuwien.endpoints;

import at.tuwien.api.semantics.EntityDto;
import at.tuwien.api.semantics.EntitySearchDto;
import at.tuwien.entities.semantics.Ontology;
import at.tuwien.exception.OntologyNotFoundException;
import at.tuwien.exception.QueryMalformedException;
import at.tuwien.service.OntologyService;
import at.tuwien.service.QueryService;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Log4j2
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/semantic")
public class QueryEndpoint {

    private final QueryService sparqlService;
    private final QueryService rdfService;
    private final OntologyService ontologyService;

    @Autowired
    public QueryEndpoint(@Qualifier("sparqlService") QueryService sparqlService,
                         @Qualifier("rdfService") QueryService rdfService, OntologyService ontologyService) {
        this.sparqlService = sparqlService;
        this.rdfService = rdfService;
        this.ontologyService = ontologyService;
    }

    @PostMapping("/ontology/{id}/query")
    @PreAuthorize("hasAuthority('execute-semantic-query')")
    @Timed(value = "semantics.sparql.execute", description = "Time needed to execute a sparql query")
    @Operation(summary = "Register a new ontology", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Executed sparql query successfully",
                    content = {@Content(
                            mediaType = "application/json")}),
    })
    public ResponseEntity<List<EntityDto>> query(@NotNull @PathVariable("id") Long id,
                                                 @NotNull @Valid @RequestBody EntitySearchDto data)
            throws OntologyNotFoundException, QueryMalformedException {
        log.debug("endpoint execute query, id={}", id);
        final Ontology ontology = ontologyService.find(id);
        final List<EntityDto> dtos;
        if (ontology.getSparqlEndpoint() != null) {
            log.debug("ontology with id {} has SPARQL endpoint", ontology.getId());
            dtos = sparqlService.find(ontology, data);
        } else {
            log.debug("ontology with id {} has RDF fallback", ontology.getId());
            dtos = rdfService.find(ontology, data);
        }
        log.trace("create ontology resulted in dtos {}", dtos);
        return ResponseEntity.ok()
                .body(dtos);
    }

}
