package at.tuwien.endpoints;

import at.tuwien.api.identifier.BibliographyTypeDto;
import at.tuwien.api.identifier.IdentifierDto;
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
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        log.debug("endpoint find identifier, pid={}, accept={}", pid, accept);
        final Identifier identifier = identifierService.find(pid);
        log.info("Found persistent identifier with id {}", identifier.getId());
        log.trace("found persistent identifier {}", identifier);
        if (accept != null) {
            log.trace("accept header present: {}", accept);
            switch (accept) {
                case "application/json":
                    log.trace("accept header matches json");
                    final IdentifierDto resource1 = identifierMapper.identifierToIdentifierDto(identifier);
                    log.debug("find identifier resulted in identifier {}", resource1);
                    return ResponseEntity.ok(resource1);
                case "text/csv":
                    log.trace("accept header matches csv");
                    final InputStreamResource resource2;
                    try {
                        resource2 = identifierService.exportResource(pid);
                        log.debug("find identifier resulted in resource {}", resource2);
                        return ResponseEntity.ok(resource2);
                    } catch (IdentifierRequestException e) {
                        /* ignore */
                    }
                case "text/xml":
                    log.trace("accept header matches xml");
                    final InputStreamResource resource3 = identifierService.exportMetadata(pid);
                    log.debug("find identifier resulted in resource {}", resource3);
                    return ResponseEntity.ok(resource3);
            }
            final Pattern regex = Pattern.compile("text\\/bibliography(; ?style=(apa|ieee|bibtex))?");
            final Matcher matcher = regex.matcher(accept);
            if (matcher.find()) {
                log.trace("accept header matches bibliography");
                final BibliographyTypeDto style;
                if (matcher.group(2) != null) {
                    style = BibliographyTypeDto.valueOf(matcher.group(2).toUpperCase());
                    log.trace("bibliography style matches {}", style);
                } else {
                    style = BibliographyTypeDto.APA;
                    log.trace("no bibliography style provided, default: {}", style);
                }
                final String resource = identifierService.exportBibliography(pid, style);
                log.debug("find identifier resulted in resource {}", resource);
                return ResponseEntity.ok(resource);
            }
        } else {
            log.trace("no accept header present");
        }
        final HttpHeaders headers = new HttpHeaders();
        final String url = identifierMapper.identifierToLocationUrl(endpointConfig.getWebsiteUrl(), identifier);
        headers.add("Location", url);
        log.debug("find identifier resulted in http redirect, headers={}, url={}", headers, url);
        return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
                .headers(headers)
                .build();
    }

}
