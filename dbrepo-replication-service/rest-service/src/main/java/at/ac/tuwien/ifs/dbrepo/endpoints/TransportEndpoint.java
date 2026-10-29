package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.replication.ReplicationPeers;
import at.ac.tuwien.ifs.dbrepo.service.transport.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.util.*;

@RestController
@RequestMapping("/api/replication/transports")
@PreAuthorize("hasAuthority('system')")
public class TransportEndpoint {
    public record Selection(@NotBlank String site, @NotNull ReplicationTransportRoutes.Transport transport) { }
    public record Delivery(@NotBlank String uri, @NotBlank String method,
                           @NotNull @Size(max = ReplicationAmqpProtocol.MAX_BYTES) byte[] body) { }
    private final ReplicationTransportRoutes routes;
    private final ReplicationAmqp rabbit;
    private final RestTemplate metadata;
    private final RestTemplate external;
    private final ReplicationPeers peers;
    private final List<String> sites;
    private final String baseUrl;

    public TransportEndpoint(ReplicationTransportRoutes routes, ReplicationAmqp rabbit,
            @Qualifier("metadataServiceRestTemplate") RestTemplate metadata,
            @Qualifier("externalReplicationRestTemplate") RestTemplate external,
            @Value("${dbrepo.replication.allowedSites:}") String allowedSites,
            @Value("${dbrepo.baseUrl}") String baseUrl) {
        this.routes = routes;
        this.rabbit = rabbit;
        this.metadata = metadata;
        this.external = external;
        this.peers = new ReplicationPeers(allowedSites);
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.sites = Arrays.stream(allowedSites.split(",")).map(String::trim).filter(s -> !s.isEmpty())
                .map(peers::requireAllowedSite).filter(s -> !s.equals(this.baseUrl)).distinct().sorted().toList();
    }

    @GetMapping
    public Map<String, Object> list() {
        return Map.of("routes", routes.findAll(), "sites", sites, "rabbitmqSites", rabbit.peers());
    }

    @PutMapping("/{databaseId}")
    public void save(@PathVariable UUID databaseId, @Valid @RequestBody Selection selection) throws IOException {
        final String site;
        try {
            site = peers.requireAllowedSite(selection.site());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        if (site.equals(baseUrl)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot replicate to the source site");
        if (selection.transport() == ReplicationTransportRoutes.Transport.RABBITMQ && !rabbit.peers().contains(site)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "RabbitMQ is not configured for this peer");
        }
        var database = metadata.getForEntity("/api/v1/database/" + databaseId, DatabaseDto.class).getBody();
        if (database == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Database not found");
        UUID remote = database.getReplicaUrls() == null ? null : database.getReplicaUrls().entrySet().stream()
                .filter(entry -> ReplicationTransportRoutes.origin(URI.create(entry.getKey())).equals(site))
                .map(Map.Entry::getValue).filter(Objects::nonNull).findFirst().orElse(null);
        routes.save(databaseId, site, selection.transport(), remote);
    }

    /** Internal subset sender uses the same peer selection as tuples and history chunks. */
    @PostMapping("/deliver")
    public ReplicationAmqpProtocol.Reply deliver(@Valid @RequestBody Delivery delivery) {
        URI uri;
        try {
            uri = URI.create(delivery.uri());
            peers.requireAllowedRequest(uri);
            ReplicationAmqpProtocol.service(delivery.method(), uri.getRawPath()
                    + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery()));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid replication destination or operation");
        }
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        try {
            var response = external.exchange(uri, HttpMethod.valueOf(delivery.method()),
                    new HttpEntity<>(delivery.body(), headers), byte[].class);
            return new ReplicationAmqpProtocol.Reply(response.getStatusCode().value(), response.getBody());
        } catch (RestClientResponseException e) {
            return new ReplicationAmqpProtocol.Reply(e.getStatusCode().value(), e.getResponseBodyAsByteArray());
        }
    }
}
