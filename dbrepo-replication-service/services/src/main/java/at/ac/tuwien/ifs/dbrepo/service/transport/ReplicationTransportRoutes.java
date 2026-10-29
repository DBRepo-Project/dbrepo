package at.ac.tuwien.ifs.dbrepo.service.transport;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;

/** Site-local configuration; owned by the same exclusive writer as the replication outbox. */
public final class ReplicationTransportRoutes {
    public enum Transport { REST, RABBITMQ }
    public record Route(UUID databaseId, String site, Transport transport, UUID remoteDatabaseId) { }

    private final ObjectMapper mapper;
    private final Path path;
    private List<Route> routes;

    public ReplicationTransportRoutes(ObjectMapper mapper, Path path) throws IOException {
        this.mapper = mapper;
        this.path = path.toAbsolutePath();
        routes = Files.exists(path) ? mapper.readValue(Files.readAllBytes(path), new TypeReference<List<Route>>() { }) : List.of();
        if (routes == null || routes.stream().anyMatch(r -> r.databaseId() == null || r.site() == null || r.transport() == null)) {
            throw new IOException("Invalid replication transport configuration");
        }
    }

    public synchronized List<Route> findAll() { return List.copyOf(routes); }

    public synchronized void save(UUID database, String site, Transport transport, UUID remote) throws IOException {
        Objects.requireNonNull(database);
        Objects.requireNonNull(site);
        Objects.requireNonNull(transport);
        UUID resolvedRemote = remote != null ? remote : routes.stream()
                .filter(r -> r.databaseId().equals(database) && r.site().equals(site))
                .map(Route::remoteDatabaseId).filter(Objects::nonNull).findFirst().orElse(null);
        var next = new ArrayList<>(routes);
        next.removeIf(r -> r.databaseId().equals(database) && r.site().equals(site));
        if (resolvedRemote != null && next.stream().anyMatch(r -> r.site().equals(site) && resolvedRemote.equals(r.remoteDatabaseId()))) {
            throw new IllegalArgumentException("Target database already belongs to another transport route");
        }
        next.add(new Route(database, site, transport, resolvedRemote));
        Files.createDirectories(path.getParent());
        Path temporary = Files.createTempFile(path.getParent(), "transports-", ".tmp");
        try {
            try (var channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                var bytes = ByteBuffer.wrap(mapper.writeValueAsBytes(next));
                while (bytes.hasRemaining()) channel.write(bytes);
                channel.force(true);
            }
            Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            try (var directory = FileChannel.open(path.getParent(), StandardOpenOption.READ)) { directory.force(true); }
            routes = List.copyOf(next);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    public synchronized boolean usesRabbit(URI uri, byte[] body) throws IOException {
        Route route = route(uri, body);
        return route != null && route.transport() == Transport.RABBITMQ;
    }

    public synchronized void created(URI uri, byte[] request, byte[] response) throws IOException {
        if (!uri.getPath().equals("/api/v1/database/replicate")) return;
        Route route = route(uri, request);
        if (route == null) return;
        UUID remote = UUID.fromString(mapper.readTree(response).required("id").asText());
        if (route.remoteDatabaseId() != null && !remote.equals(route.remoteDatabaseId())) {
            throw new IOException("Replica database identity changed");
        }
        save(route.databaseId(), route.site(), route.transport(), remote);
    }

    private Route route(URI uri, byte[] body) throws IOException {
        String origin = origin(uri);
        if (uri.getPath().equals("/api/v1/database/replicate")) {
            UUID database = UUID.fromString(mapper.readTree(body).required("creationId").asText());
            return routes.stream().filter(r -> r.site().equals(origin) && r.databaseId().equals(database)).findFirst().orElse(null);
        }
        var match = java.util.regex.Pattern.compile("^/api/v1/database/([0-9a-fA-F-]{36})(?:/.*)?$").matcher(uri.getPath());
        if (!match.matches()) return null;
        UUID database = UUID.fromString(match.group(1));
        return routes.stream().filter(r -> r.site().equals(origin) && database.equals(r.remoteDatabaseId())).findFirst().orElse(null);
    }

    public static String origin(URI uri) {
        String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
        int port = uri.getPort();
        boolean standard = port == -1 || scheme.equals("https") && port == 443 || scheme.equals("http") && port == 80;
        return scheme + "://" + uri.getHost().toLowerCase(Locale.ROOT) + (standard ? "" : ":" + port);
    }
}
