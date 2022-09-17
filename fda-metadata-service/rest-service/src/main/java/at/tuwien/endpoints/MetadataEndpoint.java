package at.tuwien.endpoints;

import at.tuwien.service.MetadataService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

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

    @GetMapping
    @Operation(summary = "Identify the repository")
    public ResponseEntity<?> identify(UriComponentsBuilder uriComponentsBuilder) {
        final URI uri = uriComponentsBuilder
                .replacePath(null)
                .replaceQuery(null)
                .build().toUri();
        final String document = metadataService.identify(uri.toString());
        return ResponseEntity.ok(document);
    }

}
