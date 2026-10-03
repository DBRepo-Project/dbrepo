package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.replication.ReplicationSites;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReplicationSitesUnitTest {

    @Test
    void legacyOriginsRemainWritable() {
        assertFalse(ReplicationSites.isReplica(null, "https://local.example"));
        assertFalse(ReplicationSites.isReplica("  ", "https://local.example"));
    }

    @Test
    void equivalentLocalOriginsRemainWritable() {
        assertFalse(ReplicationSites.isReplica(" HTTPS://LOCAL.example:443/// ", "https://local.example"));
        assertFalse(ReplicationSites.isReplica("http://local.example:80/", "http://local.example"));
    }

    @Test
    void differentOrMalformedOriginsAreReadOnly() {
        assertTrue(ReplicationSites.isReplica("https://remote.example", "https://local.example"));
        assertTrue(ReplicationSites.isReplica("http://local.example", "https://local.example"));
        assertTrue(ReplicationSites.isReplica("https://local.example:8443", "https://local.example"));
        assertTrue(ReplicationSites.isReplica("invalid origin", "https://local.example"));
        assertTrue(ReplicationSites.isReplica("https://local.example", null));
    }
}
