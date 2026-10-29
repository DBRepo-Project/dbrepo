package at.ac.tuwien.ifs.dbrepo.service.transport;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.time.Instant;
import java.util.Set;

public final class ReplicationAmqpProtocol {
    public static final int MAX_BYTES = 16 * 1024 * 1024;
    private static final String ID = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";
    private static final String DB = "/api/v1/database/" + ID;
    public record Request(String method, String path, byte[] body) { }
    public record Reply(int status, byte[] body) {
        public ClientHttpResponse response() {
            return new ClientHttpResponse() {
                @Override public HttpStatusCode getStatusCode() { return HttpStatusCode.valueOf(status); }
                @Override public String getStatusText() { return Integer.toString(status); }
                @Override public void close() { }
                @Override public InputStream getBody() { return new ByteArrayInputStream(body == null ? new byte[0] : body); }
                @Override public HttpHeaders getHeaders() {
                    var headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    return headers;
                }
            };
        }
    }

    private ReplicationAmqpProtocol() { }

    /** Never turn a broker message into an unrestricted HTTP proxy. */
    public static String service(String method, String value) {
        if (method == null || value == null) throw new IllegalArgumentException("Missing replication operation");
        URI uri = URI.create(value);
        String path = uri.getRawPath();
        if (uri.isAbsolute() || uri.getRawAuthority() != null || uri.getRawFragment() != null || path == null
                || path.contains("%") || path.contains("..") || path.contains("//")) {
            throw new IllegalArgumentException("Invalid replication path");
        }
        if (uri.getRawQuery() != null) {
            if (!method.equals("DELETE") || !path.matches(DB + "/table/" + ID + "/replicate")
                    || !uri.getRawQuery().startsWith("archivedAt=")) throw new IllegalArgumentException("Invalid replication query");
            Instant.parse(uri.getRawQuery().substring("archivedAt=".length()));
        }
        if (method.equals("POST") && (path.equals("/api/v1/database/replicate")
                || path.matches(DB + "/(table|view)/replicate"))) return "metadata";
        if (method.equals("DELETE") && path.matches(DB + "/table/" + ID + "/replicate")) return "metadata";
        if (method.equals("PUT") && path.matches(DB + "(/table/" + ID + ")?/replication-url")) return "metadata";
        if (Set.of("POST", "PUT", "DELETE").contains(method) && path.matches(DB + "/table/" + ID + "/data/replicate")) return "data";
        if (Set.of("POST", "PUT", "PATCH").contains(method) && path.matches(DB + "/table/" + ID + "/timestamps")) return "data";
        if (method.equals("PUT") && path.matches(DB + "/subset/replicate")) return "data";
        if (method.equals("POST") && path.matches(DB + "/replication/snapshots/(imports|" + ID + "/reconcile)")) return "data";
        if (method.equals("PUT") && path.matches(DB + "/replication/snapshots/" + ID + "/chunks/[0-9]+")) return "data";
        if (method.equals("GET") && path.matches(DB + "/replication/snapshots/table/" + ID + "/checkpoint-with-inbox")) return "data";
        throw new IllegalArgumentException("Operation is not a replication endpoint");
    }
}
