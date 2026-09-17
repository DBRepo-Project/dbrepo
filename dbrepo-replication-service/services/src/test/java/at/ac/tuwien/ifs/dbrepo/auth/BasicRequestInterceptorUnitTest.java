package at.ac.tuwien.ifs.dbrepo.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BasicRequestInterceptorUnitTest {

    @Mock
    private HttpRequest request;

    @Mock
    private ClientHttpRequestExecution execution;

    @Mock
    private ClientHttpResponse response;

    @Test
    public void intercept_setsConfiguredCredentials() throws IOException {
        final HttpHeaders headers = new HttpHeaders();
        final byte[] body = new byte[0];
        when(request.getHeaders()).thenReturn(headers);
        when(execution.execute(request, body)).thenReturn(response);

        new BasicRequestInterceptor("replication", "secret").intercept(request, body, execution);

        assertEquals("Basic cmVwbGljYXRpb246c2VjcmV0", headers.getFirst(HttpHeaders.AUTHORIZATION));
        verify(execution).execute(request, body);
    }
}
