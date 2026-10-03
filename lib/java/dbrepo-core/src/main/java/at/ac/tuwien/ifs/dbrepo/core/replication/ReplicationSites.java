package at.ac.tuwien.ifs.dbrepo.core.replication;

import java.net.URI;
import java.util.Locale;

public final class ReplicationSites {

    private ReplicationSites() {
    }

    public static boolean isReplica(String creationLocation, String localSite) {
        // Databases created before replication was introduced have no origin.
        if (creationLocation == null || creationLocation.isBlank()) {
            return false;
        }
        try {
            return !normalize(creationLocation).equals(normalize(localSite));
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    private static String normalize(String site) {
        if (site == null) {
            throw new IllegalArgumentException("Missing local site");
        }
        final URI uri = URI.create(site.trim());
        if (uri.getHost() == null || uri.getScheme() == null || uri.getUserInfo() != null
                || uri.getQuery() != null || uri.getFragment() != null) {
            throw new IllegalArgumentException("Invalid site URL");
        }
        final String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("http") && !scheme.equals("https")) {
            throw new IllegalArgumentException("Invalid site scheme");
        }
        final int port = uri.getPort();
        final boolean defaultPort = port == -1 || (scheme.equals("https") ? port == 443 : port == 80);
        return scheme + "://" + uri.getHost().toLowerCase(Locale.ROOT)
                + (defaultPort ? "" : ":" + port) + uri.getPath().replaceAll("/+$", "");
    }
}
