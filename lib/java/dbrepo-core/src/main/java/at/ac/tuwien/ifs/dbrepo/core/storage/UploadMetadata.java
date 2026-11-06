package at.ac.tuwien.ifs.dbrepo.core.storage;

import at.ac.tuwien.ifs.dbrepo.core.exception.NotAllowedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/** Ownership and filename metadata for temporary user uploads. */
public final class UploadMetadata {
    public static final String OWNER = "upload-owner";
    public static final String FILENAME = "original-filename-base64";
    public static final String SYSTEM_OWNER = "system";
    private static final List<String> COMPRESSION_SUFFIXES = List.of(".gz", ".bz2", ".deflate", ".snappy", ".lz4", ".zst");
    private static final Pattern KEY = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(csv|tsv)(\\.(gz|bz2|deflate|snappy|lz4|zst))?");

    private UploadMetadata() {
    }

    public static String ownerId(Principal principal) throws NotAllowedException {
        if (!(principal instanceof Authentication authentication) || !authentication.isAuthenticated()) {
            throw new NotAllowedException("Upload ownership requires an authenticated caller");
        }
        if (authentication.getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals("system"))) {
            return SYSTEM_OWNER;
        }
        if (authentication.getPrincipal() instanceof Jwt jwt && jwt.getSubject() != null) {
            try {
                final String owner = UUID.fromString(jwt.getSubject()).toString();
                if (owner.equals(jwt.getSubject())) {
                    return owner;
                }
            } catch (IllegalArgumentException ignored) {
                // Only the validated stable subject identifies an ordinary upload owner.
            }
        }
        throw new NotAllowedException("Upload ownership requires a valid subject");
    }

    public static String newKey(String originalFilename) {
        String name = originalFilename == null ? "" : originalFilename.toLowerCase(Locale.ROOT);
        final String compression = COMPRESSION_SUFFIXES.stream().filter(name::endsWith).findFirst().orElse("");
        name = name.substring(0, name.length() - compression.length());
        // DuckDB's table replacement scan needs a CSV/TSV suffix; byte consumers ignore it.
        return UUID.randomUUID() + (name.endsWith(".tsv") ? ".tsv" : ".csv") + compression;
    }

    public static Map<String, String> metadata(String originalFilename, String owner) {
        final String filename = originalFilename == null ? "" : originalFilename;
        return Map.of(OWNER, owner, FILENAME, Base64.getEncoder().encodeToString(filename.getBytes(StandardCharsets.UTF_8)));
    }

    public static boolean isUploadKey(String key) {
        return key != null && KEY.matcher(key).matches();
    }
}
