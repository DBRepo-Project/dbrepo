package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.service.HistorySnapshotCodec;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;
import org.springframework.web.server.ResponseStatusException;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;

/** Bound JSON/base64 decoding before allocation, including requests with no Content-Length. */
@ControllerAdvice(assignableTypes = HistorySnapshotEndpoint.class)
public class HistorySnapshotBodyLimit extends RequestBodyAdviceAdapter {
    public static final long MAX_WIRE_BYTES = ((HistorySnapshotCodec.MAX_CHUNK_BYTES + 2L) / 3) * 4 + 65536;

    @Override
    public boolean supports(MethodParameter parameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        return parameter.getContainingClass() == HistorySnapshotEndpoint.class;
    }

    @Override
    public HttpInputMessage beforeBodyRead(HttpInputMessage input, MethodParameter parameter, Type targetType,
                                           Class<? extends HttpMessageConverter<?>> converterType) throws IOException {
        if (input.getHeaders().getContentLength() > MAX_WIRE_BYTES) throw tooLarge();
        final InputStream limited = new FilterInputStream(input.getBody()) {
            private long consumed;
            @Override public int read() throws IOException {
                final int value = in.read();
                if (value != -1 && ++consumed > MAX_WIRE_BYTES) throw tooLarge();
                return value;
            }
            @Override public int read(byte[] bytes, int offset, int length) throws IOException {
                final int read = in.read(bytes, offset, (int) Math.min(length, MAX_WIRE_BYTES - consumed + 1));
                if (read > 0 && (consumed += read) > MAX_WIRE_BYTES) throw tooLarge();
                return read;
            }
        };
        return new HttpInputMessage() {
            @Override public InputStream getBody() { return limited; }
            @Override public HttpHeaders getHeaders() { return input.getHeaders(); }
        };
    }

    private static ResponseStatusException tooLarge() {
        return new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Snapshot request exceeds the bounded wire size");
    }
}
