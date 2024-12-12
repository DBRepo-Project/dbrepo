package at.tuwien.mvc;

import at.tuwien.api.error.ApiErrorDto;
import at.tuwien.endpoints.*;
import at.tuwien.test.AbstractUnitTest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Log4j2
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@SpringBootTest
public class OpenApiEndpointMvcTest extends AbstractUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void openApiDocs_succeeds() throws Exception {
        this.mockMvc.perform(get("/v3/api-docs.yaml"))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    public void openApiDocs_accessEndpointApiResponses_succeeds() {
        generic_openApiDocs(AccessEndpoint.class);
    }

    @Test
    public void openApiDocs_databaseEndpointApiResponses_succeeds() {
        generic_openApiDocs(DatabaseEndpoint.class);
    }

    @Test
    public void openApiDocs_subsetEndpointApiResponses_succeeds() {
        generic_openApiDocs(SubsetEndpoint.class);
    }

    @Test
    public void openApiDocs_tableEndpointApiResponses_succeeds() {
        generic_openApiDocs(TableEndpoint.class);
    }

    @Test
    public void openApiDocs_viewEndpointApiResponses_succeeds() {
        generic_openApiDocs(ViewEndpoint.class);
    }

    private void generic_openApiDocs(Class<?> endpoint) {
        final List<Method> methods = Arrays.stream(endpoint.getMethods())
                .filter(m -> m.getDeclaringClass().equals(endpoint))
                .toList();
        methods.forEach(m -> {
            assertNotNull(m.getDeclaredAnnotation(Operation.class).summary());
            final List<Class<?>> exceptions = Arrays.stream(m.getExceptionTypes())
                    .toList();
            final List<Class<?>> invalidExceptions = exceptions.stream()
                    .filter(e -> !e.getName().startsWith("at.tuwien."))
                    .toList();
            assertTrue(invalidExceptions.isEmpty(), "method '" + m.getName() + "' throws exception(s) outside package scope at.tuwien: " + invalidExceptions.stream().map(Class::getName).toList());
            exceptions.forEach(exception -> {
                final int status = exception.getAnnotation(ResponseStatus.class)
                        .code()
                        .value();
                final List<ApiResponse> responses = Arrays.stream(m.getDeclaredAnnotationsByType(ApiResponse.class))
                        .filter(r -> status == Integer.parseInt(r.responseCode()))
                        .toList();
                assertFalse(responses.isEmpty(), "missing openapi docs on method '" + m.getName() + "' for http " + status + " status");
                responses.forEach(response -> {
                    assertNotNull(response.description());
                    assertTrue(response.description().length() > 3) /* meaningful description */;
                });
                if (status >= 300) {
                    /* consistent error responses */
                    responses.forEach(response -> {
                        assertNotNull(response.content());
                        assertTrue(response.content().length > 0);
                        final Content content0 = response.content()[0];
                        assertEquals(MediaType.APPLICATION_JSON_VALUE, content0.mediaType());
                        assertEquals(ApiErrorDto.class, content0.schema().implementation());
                    });
                }
            });
        });
    }

}
