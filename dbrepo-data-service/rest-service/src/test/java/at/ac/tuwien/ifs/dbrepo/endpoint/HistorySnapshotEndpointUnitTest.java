package at.ac.tuwien.ifs.dbrepo.endpoint;

import at.ac.tuwien.ifs.dbrepo.core.api.replication.HistorySnapshotDto.Envelope;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.endpoints.HistorySnapshotEndpoint;
import at.ac.tuwien.ifs.dbrepo.endpoints.HistorySnapshotBodyLimit;
import at.ac.tuwien.ifs.dbrepo.service.HistorySnapshotService;
import at.ac.tuwien.ifs.dbrepo.service.MetadataService;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HistorySnapshotEndpointUnitTest {
    @Configuration
    @EnableMethodSecurity
    static class Config {
        @Bean MetadataService metadata() { return mock(MetadataService.class); }
        @Bean HistorySnapshotService snapshots() { return mock(HistorySnapshotService.class); }
        @Bean HistorySnapshotEndpoint endpoint(MetadataService metadata, HistorySnapshotService snapshots) {
            return new HistorySnapshotEndpoint(metadata, snapshots);
        }
    }

    @Test
    void methodSecurityRequiresReplicationOrSystemEvenForGet() throws Exception {
        try (var context = new AnnotationConfigApplicationContext(Config.class)) {
            final var endpoint = context.getBean(HistorySnapshotEndpoint.class);
            final var metadata = context.getBean(MetadataService.class);
            final var snapshots = context.getBean(HistorySnapshotService.class);
            final UUID databaseId = UUID.randomUUID(), snapshotId = UUID.randomUUID();
            final var database = Database.builder().id(databaseId).build();
            final var result = new Envelope(null, "test");
            when(metadata.getDatabase(databaseId)).thenReturn(database);
            when(snapshots.manifest(database, snapshotId)).thenReturn(result);
            SecurityContextHolder.clearContext();
            assertThrows(AuthenticationCredentialsNotFoundException.class, () -> endpoint.manifest(databaseId, snapshotId));
            authenticate("read");
            assertThrows(AccessDeniedException.class, () -> endpoint.manifest(databaseId, snapshotId));
            verifyNoInteractions(metadata, snapshots);
            for (String role : List.of("replication", "system")) {
                authenticate(role);
                assertSame(result, endpoint.manifest(databaseId, snapshotId));
            }
        } finally { SecurityContextHolder.clearContext(); }
    }

    @Test
    void wireLimitRejectsOversizeBeforeAndDuringJsonDecoding() throws Exception {
        final var advice = new HistorySnapshotBodyLimit();
        final var headers = new HttpHeaders();
        headers.setContentLength(HistorySnapshotBodyLimit.MAX_WIRE_BYTES + 1);
        final HttpInputMessage input = new HttpInputMessage() {
            @Override public InputStream getBody() {
                return new InputStream() { @Override public int read() { return 'x'; } };
            }
            @Override public HttpHeaders getHeaders() { return headers; }
        };
        assertThrows(ResponseStatusException.class, () -> advice.beforeBodyRead(input, null, null, null));
        headers.remove(HttpHeaders.CONTENT_LENGTH);
        final InputStream bounded = advice.beforeBodyRead(input, null, null, null).getBody();
        assertThrows(ResponseStatusException.class, () -> bounded.transferTo(java.io.OutputStream.nullOutputStream()));
    }

    private static void authenticate(String role) {
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                "test", "unused", List.of(new SimpleGrantedAuthority(role))));
    }
}
