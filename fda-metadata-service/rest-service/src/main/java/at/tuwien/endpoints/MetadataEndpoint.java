package at.tuwien.endpoints;

import at.tuwien.OaiErrorType;
import at.tuwien.OaiListIdentifiersParameters;
import at.tuwien.OaiListRecordsParameters;
import at.tuwien.service.MetadataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.ws.rs.core.Response;

@Log4j2
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/oai")
public class MetadataEndpoint extends AbstractEndpoint {

    private final MetadataService metadataService;

    @Autowired
    public MetadataEndpoint(MetadataService metadataService) {
        this.metadataService = metadataService;
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

}
