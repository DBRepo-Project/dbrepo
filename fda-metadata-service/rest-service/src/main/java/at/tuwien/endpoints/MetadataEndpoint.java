package at.tuwien.endpoints;

import at.tuwien.OaiErrorType;
import at.tuwien.OaiListIdentifiersParameters;
import at.tuwien.OaiRecordParameters;
import at.tuwien.exception.IdentifierNotFoundException;
import at.tuwien.service.IdentifierService;
import at.tuwien.service.MetadataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
public class MetadataEndpoint extends AbstractEndpoint {

    private final MetadataService metadataService;
    private final IdentifierService identifierService;

    @Autowired
    public MetadataEndpoint(MetadataService metadataService, IdentifierService identifierService) {
        this.metadataService = metadataService;
        this.identifierService = identifierService;
    }

    @GetMapping(produces = "text/xml;charset=UTF-8")
    @Operation(summary = "Identify the repository")
    public ResponseEntity<?> identify() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(metadataService.error(OaiErrorType.BAD_VERB));
    }

    @GetMapping(params = "verb=Identify", produces = "text/xml;charset=UTF-8")
    @Operation(summary = "Identify the repository")
    @Parameter(name = "verb", example = "Identify")
    public ResponseEntity<?> identifyAlt() {
        return ResponseEntity.ok(metadataService.identify());
    }

    @GetMapping(params = "verb=ListIdentifiers", produces = "text/xml;charset=UTF-8")
    @Operation(summary = "List the identifiers")
    @Parameter(name = "verb", example = "ListIdentifiers")
    public ResponseEntity<?> listRecords(OaiListIdentifiersParameters parameters) {
        return ResponseEntity.ok(metadataService.listIdentifiers(parameters));
    }

    @GetMapping(params = "verb=GetRecord", produces = "text/xml;charset=UTF-8")
    @Operation(summary = "Get the record")
    @Parameter(name = "verb", example = "GetRecord")
    public ResponseEntity<?> getRecord(OaiRecordParameters parameters) {
        if (!parameters.getMetadataPrefix().equals("oai_dc")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(metadataService.error(OaiErrorType.CANNOT_DISSEMINATE_FORMAT));
        }
        if (parameters.getIdentifier() == null || !NumberUtils.isCreatable(parameters.getIdentifier())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(metadataService.error(OaiErrorType.NO_RECORDS_MATCH));
        }
        try {
            return ResponseEntity.ok(metadataService.getRecord(parameters));
        } catch (IdentifierNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(metadataService.error(OaiErrorType.ID_DOES_NOT_EXIST));
        }
    }

    @GetMapping(params = "verb=ListMetadataFormats", produces = "text/xml;charset=UTF-8")
    @Operation(summary = "List the metadata formats")
    @Parameter(name = "verb", example = "ListMetadataFormats")
    public ResponseEntity<?> listMetadataFormats() {
        return ResponseEntity.ok(metadataService.listMetadataFormats());
    }

}
