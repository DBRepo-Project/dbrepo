package at.ac.tuwien.ifs.dbrepo.config;

import at.ac.tuwien.ifs.dbrepo.core.replication.ReplicationPeers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

class ReplicationPeersUnitTest {

    private final ReplicationPeers peers = new ReplicationPeers(" https://PEER.example:443/, https://other.example:8443 ");

    @Test
    void normalizesOriginsAndAllowsApiPaths() {
        assertEquals("https://peer.example", peers.requireAllowedSite("https://PEER.example:443/"));
        assertEquals("https://other.example:8443", peers.requireAllowedSite("https://other.example:8443"));
        assertDoesNotThrow(() -> peers.requireAllowedRequest(URI.create("https://peer.example/api/v1/database?limit=100")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://untrusted.example", "https://peer.example.evil.example", "https://peer.example:8443",
            "https://peer.example@evil.example", "https://user@peer.example", "https://peer.example#fragment",
            "http://peer.example", "file:///etc/passwd", "//peer.example", "https://peer.example:0", "https://peer.example:65536"})
    void rejectsUntrustedRequestOrigins(String uri) {
        assertThrows(IllegalArgumentException.class, () -> peers.requireAllowedRequest(URI.create(uri)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://peer.example/api", "https://peer.example?next=evil", "https://peer.example#fragment",
            "http://peer.example", "https://user:secret@peer.example", "https://peer.example:", ""})
    void rejectsInvalidSiteConfigurationAndInput(String site) {
        assertThrows(IllegalArgumentException.class, () -> peers.requireAllowedSite(site));
        if (!site.isEmpty()) {
            assertThrows(IllegalArgumentException.class, () -> new ReplicationPeers(site));
        }
    }

    @Test
    void emptyConfigurationDeniesAllDestinations() {
        assertThrows(IllegalArgumentException.class,
                () -> new ReplicationPeers("").requireAllowedSite("https://peer.example"));
        assertThrows(IllegalArgumentException.class, () -> peers.requireAllowedSite(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"http://localhost:8080", "http://127.0.0.1:8080", "http://[::1]:8080"})
    void loopbackHttpRequiresExplicitConfiguration(String site) {
        assertThrows(IllegalArgumentException.class, () -> peers.requireAllowedSite(site));
        assertEquals(site, new ReplicationPeers(site).requireAllowedSite(site));
    }
}
