package at.ac.tuwien.ifs.dbrepo.service.transport;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.*;

import java.io.IOException;

public final class ReplicationPeerTransport implements ClientHttpRequestInterceptor {
    private final ReplicationTransportRoutes routes;
    private final ReplicationAmqp rabbit;

    public ReplicationPeerTransport(ReplicationTransportRoutes routes, ReplicationAmqp rabbit) {
        this.routes = routes;
        this.rabbit = rabbit;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution http) throws IOException {
        var uri = request.getURI();
        if (!routes.usesRabbit(uri, body)) return http.execute(request, body);
        String path = uri.getRawPath() + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());
        var reply = rabbit.send(ReplicationTransportRoutes.origin(uri),
                new ReplicationAmqpProtocol.Request(request.getMethod().name(), path, body));
        if (reply.status() >= 200 && reply.status() < 300) routes.created(uri, body, reply.body());
        return reply.response();
    }
}
