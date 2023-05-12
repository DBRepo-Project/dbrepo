package at.tuwien.endpoints;

import at.tuwien.exception.IdentifierNotFoundException;
import at.tuwien.oaipmh.OaiErrorType;
import at.tuwien.oaipmh.OaiListIdentifiersParameters;
import at.tuwien.oaipmh.OaiRecordParameters;
import at.tuwien.service.MetadataService;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Log4j2
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/oai")
public class MetadataEndpoint {

    private final MetadataService metadataService;

    @Autowired
    public MetadataEndpoint(MetadataService metadataService) {
        this.metadataService = metadataService;
    }

    @GetMapping(produces = "text/xml;charset=UTF-8")
    @Parameter(name = "verb", in = ParameterIn.QUERY, examples = {
            @ExampleObject(value = "Identify"),
            @ExampleObject(value = "ListIdentifiers"),
            @ExampleObject(value = "GetRecord"),
            @ExampleObject(value = "ListMetadataFormats"),
    })
    @Timed(value = "repository.identify", description = "Time needed to identify the repository")
    @Operation(summary = "Identify the repository")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "List containers",
                    content = {@Content(mediaType = "text/xml")}),
    })
    public ResponseEntity<String> identify() {
        log.debug("endpoint identify repository");
        return identifyAlt();
    }

    @GetMapping(params = "verb=Identify", produces = "text/xml;charset=UTF-8")
    @Timed(value = "repository.identify", description = "Time needed to identify the repository")
    @Operation(summary = "Identify the repository")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "List containers",
                    content = {@Content(mediaType = "text/xml")}),
    })
    public ResponseEntity<String> identifyAlt() {
        log.debug("endpoint identify repository, verb=Identify");
        final String xml = metadataService.identify();
        log.trace("identify repository resulted in xml {}", xml);
        return ResponseEntity.ok(xml);
    }

    @GetMapping(params = "verb=ListIdentifiers", produces = "text/xml;charset=UTF-8")
    @Timed(value = "identifiers.list", description = "Time needed to list the identifiers")
    @Operation(summary = "List the identifiers")
    public ResponseEntity<String> listIdentifiers(OaiListIdentifiersParameters parameters) {
        log.debug("endpoint list identifiers, verb=ListIdentifiers, parameters={}", parameters);
        final String xml = metadataService.listIdentifiers(parameters);
        log.trace("list identifiers resulted in xml {}", xml);
        return ResponseEntity.ok(xml);
    }

    @GetMapping(params = "verb=GetRecord", produces = "text/xml;charset=UTF-8")
    @Timed(value = "record.find", description = "Time needed to find a record")
    @Operation(summary = "Get the record")
    public ResponseEntity<String> getRecord(OaiRecordParameters parameters) {
        log.debug("endpoint get record, verb=GetRecord, parameters={}", parameters);
        if (parameters.getMetadataPrefix() != null && !parameters.getMetadataPrefix().equals("oai_dc")) {
            log.trace("metadataPrefix matches oai_dc, failed to serve this format");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(metadataService.error(OaiErrorType.CANNOT_DISSEMINATE_FORMAT));
        }
        if (parameters.getIdentifier() == null || !NumberUtils.isCreatable(parameters.getIdentifier())) {
            log.trace("identifier is null or not a number");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(metadataService.error(OaiErrorType.NO_RECORDS_MATCH));
        }
        try {
            final String xml = metadataService.getRecord(parameters);
            log.trace("get record resulted in xml {}", xml);
            return ResponseEntity.ok(xml);
        } catch (IdentifierNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(metadataService.error(OaiErrorType.ID_DOES_NOT_EXIST));
        }
    }

    @GetMapping(params = "verb=ListMetadataFormats", produces = "text/xml;charset=UTF-8")
    @Timed(value = "formats.list", description = "Time needed to list the metadata formats")
    @Operation(summary = "List the metadata formats")
    public ResponseEntity<String> listMetadataFormats() {
        log.debug("endpoint list metadata formats, verb=ListMetadataFormats");
        final String xml = metadataService.listMetadataFormats();
        log.trace("list metadata formats resulted in xml {}", xml);
        return ResponseEntity.ok(xml);
    }

}
