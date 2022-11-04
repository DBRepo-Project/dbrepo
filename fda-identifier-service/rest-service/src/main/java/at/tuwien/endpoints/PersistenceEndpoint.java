package at.tuwien.endpoints;

import at.tuwien.config.EndpointConfig;
import at.tuwien.entities.identifier.Identifier;
import at.tuwien.exception.IdentifierNotFoundException;
import at.tuwien.exception.IdentifierRequestException;
import at.tuwien.exception.QueryNotFoundException;
import at.tuwien.exception.RemoteUnavailableException;
import at.tuwien.mapper.IdentifierMapper;
import at.tuwien.service.IdentifierService;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Log4j2
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/pid")
public class PersistenceEndpoint {

    private final EndpointConfig endpointConfig;
    private final IdentifierMapper identifierMapper;
    private final IdentifierService identifierService;

    @Autowired
    public PersistenceEndpoint(EndpointConfig endpointConfig, IdentifierMapper identifierMapper, IdentifierService identifierService) {
        this.endpointConfig = endpointConfig;
        this.identifierMapper = identifierMapper;
        this.identifierService = identifierService;
    }

    @GetMapping("/{pid}")
    @Transactional(readOnly = true)
    @Timed(value = "pid.find", description = "Time needed to find a persisted identifier")
    @Operation(summary = "Find some identifier")
    public ResponseEntity<?> find(@Valid @PathVariable("pid") Long pid,
                                  @RequestHeader(HttpHeaders.ACCEPT) String accept) throws IdentifierNotFoundException,
            QueryNotFoundException, RemoteUnavailableException, IdentifierRequestException {
        log.debug("find identifier endpoint, pid={}, accept={}", pid, accept);
        final Identifier identifier = identifierService.find(pid);
        log.info("Found persistent identifier with id {}", identifier.getId());
        log.trace("found persistent identifier {}", identifier);
        if (accept != null) {
            log.trace("accept header present: {}", accept);
            if (accept.equals("application/json")) {
                log.trace("accept header matches json");
                return ResponseEntity.ok(identifierMapper.identifierToIdentifierDto(identifier));
            } else if (accept.equals("text/csv")) {
                log.trace("accept header matches csv");
                return ResponseEntity.ok(identifierService.exportResource(pid));
            }
        }
        log.trace("no accept header present, serving http redirect");
        final HttpHeaders headers = new HttpHeaders();
        headers.add("Location", identifierMapper.identifierToLocationUrl(endpointConfig.getWebsiteUrl(), identifier));
        return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
                .headers(headers)
                .build();
    }

}
