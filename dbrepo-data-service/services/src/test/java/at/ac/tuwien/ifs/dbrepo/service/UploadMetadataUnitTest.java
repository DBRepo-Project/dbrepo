package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.exception.NotAllowedException;
import at.ac.tuwien.ifs.dbrepo.core.storage.UploadMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class UploadMetadataUnitTest {
    private static final String OWNER = "b3468012-3375-4d80-b6b0-58b72f96594f";

    private JwtAuthenticationToken token(String subject, String username, String uid) {
        var builder = Jwt.withTokenValue("validated-token").header("alg", "RS256")
                .claim("preferred_username", username).claim("uid", uid);
        if (subject != null) builder.subject(subject);
        return new JwtAuthenticationToken(builder.build(), List.of());
    }

    @Test
    void subjectIsStableAcrossUsernameChangesAndDoesNotUseUid() throws Exception {
        assertEquals(OWNER, UploadMetadata.ownerId(token(OWNER, "min", "unrelated-id")));
        assertEquals(OWNER, UploadMetadata.ownerId(token(OWNER, "renamed", UUID.randomUUID().toString())));
        String other = UUID.randomUUID().toString();
        assertEquals(other, UploadMetadata.ownerId(token(other, "admin", OWNER)));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"not-a-uuid", "1-1-1-1-1", "B3468012-3375-4D80-B6B0-58B72F96594F", " b3468012-3375-4d80-b6b0-58b72f96594f"})
    void malformedOrMissingSubjectIsDenied(String subject) {
        assertThrows(NotAllowedException.class, () -> UploadMetadata.ownerId(token(subject, "admin", OWNER)));
    }

    @Test
    void onlyAuthenticatedExactSystemAuthorityBypassesSubject() throws Exception {
        var authority = List.of(new SimpleGrantedAuthority("system"));
        var system = UsernamePasswordAuthenticationToken.authenticated("internal", "unused", authority);
        assertEquals(UploadMetadata.SYSTEM_OWNER, UploadMetadata.ownerId(system));
        system.setAuthenticated(false);
        assertThrows(NotAllowedException.class, () -> UploadMetadata.ownerId(system));
        assertThrows(NotAllowedException.class, () -> UploadMetadata.ownerId(null));
        assertThrows(NotAllowedException.class, () -> UploadMetadata.ownerId(
                UsernamePasswordAuthenticationToken.authenticated("admin", "unused", List.of(new SimpleGrantedAuthority("ROLE_SYSTEM")))));
    }

    @ParameterizedTest
    @ValueSource(strings = {"measurements.csv", "data.tsv", "data.CSV.GZ", "data.tsv.gz", "data", "data.txt", "data.gz", "../O'Brien ä.csv", "image.png", "data.csv.bz2", "data.csv.zst"})
    void generatedKeysRetainOnlyFixedReaderSuffixes(String filename) {
        String key = UploadMetadata.newKey(filename);
        assertTrue(UploadMetadata.isUploadKey(key));
        assertNotEquals(key, UploadMetadata.newKey(filename));
        assertFalse(key.contains("/"));
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".gz")) assertTrue(key.endsWith(".gz"));
        if (lower.contains(".tsv")) assertTrue(key.contains(".tsv"));
        if (lower.equals("data.txt") || lower.equals("image.png") || lower.equals("data")) assertTrue(key.endsWith(".csv"));
        assertEquals(filename, new String(Base64.getDecoder().decode(UploadMetadata.metadata(filename, OWNER)
                .get(UploadMetadata.FILENAME)), StandardCharsets.UTF_8));
        assertEquals(OWNER, UploadMetadata.metadata(filename, OWNER).get(UploadMetadata.OWNER));
    }
}
