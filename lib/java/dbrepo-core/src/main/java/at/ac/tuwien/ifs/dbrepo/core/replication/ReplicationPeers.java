package at.ac.tuwien.ifs.dbrepo.core.replication;

import java.net.URI;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public final class ReplicationPeers {

    private final Set<String> allowedSites;

    public ReplicationPeers(String sites) {
        allowedSites = Arrays.stream(sites.split(","))
                .map(String::trim)
                .filter(site -> !site.isEmpty())
                .map(ReplicationPeers::siteOrigin)
                .collect(Collectors.toUnmodifiableSet());
    }

    public String requireAllowedSite(String site) {
        final String origin = siteOrigin(site);
        requireAllowedOrigin(origin);
        return origin;
    }

    public void requireAllowedRequest(URI uri) {
        requireAllowedOrigin(origin(uri));
    }

    private void requireAllowedOrigin(String origin) {
        if (!allowedSites.contains(origin)) {
            throw new IllegalArgumentException("Replication site is not configured in REPLICATION_ALLOWED_SITES: " + origin);
        }
    }

    private static String siteOrigin(String site) {
        if (site == null) {
            throw new IllegalArgumentException("Replication site must not be null");
        }
        final URI uri = URI.create(site.trim());
        if (uri.getRawQuery() != null || (uri.getRawPath() != null && !uri.getRawPath().isEmpty()
                && !uri.getRawPath().equals("/"))) {
            throw new IllegalArgumentException("Replication site must be an origin without a path or query");
        }
        return origin(uri);
    }

    private static String origin(URI uri) {
        if (uri.getScheme() == null || uri.getHost() == null || uri.getRawUserInfo() != null
                || uri.getRawFragment() != null || uri.getPort() == 0 || uri.getPort() > 65535
                || uri.getRawAuthority().endsWith(":")) {
            throw new IllegalArgumentException("Invalid replication site origin");
        }
        final String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
        final String host = uri.getHost().toLowerCase(Locale.ROOT);
        final boolean loopback = Set.of("localhost", "127.0.0.1", "[::1]").contains(host);
        if (!scheme.equals("https") && !(scheme.equals("http") && loopback)) {
            throw new IllegalArgumentException("Replication sites require HTTPS (HTTP is only allowed on loopback)");
        }
        final int port = uri.getPort();
        final boolean defaultPort = port == -1 || (scheme.equals("https") ? port == 443 : port == 80);
        return scheme + "://" + host + (defaultPort ? "" : ":" + port);
    }
}
