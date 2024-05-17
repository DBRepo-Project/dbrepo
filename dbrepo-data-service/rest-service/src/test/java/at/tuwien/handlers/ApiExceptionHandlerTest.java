package at.tuwien.handlers;

import at.tuwien.test.AbstractUnitTest;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static at.tuwien.test.utils.EndpointUtils.getErrorCodes;
import static at.tuwien.test.utils.EndpointUtils.getExceptions;

@Log4j2
@ExtendWith(SpringExtension.class)
@SpringBootTest
public class ApiExceptionHandlerTest extends AbstractUnitTest {

    @Test
    public void handle_succeeds() throws ClassNotFoundException, IOException {
        final List<Method> handlers = Arrays.asList(ApiExceptionHandler.class.getMethods());
        final List<String> errorCodes = getErrorCodes();

        /* test */
        for (Class<?> exception : getExceptions()) {
            final Optional<Method> optional = handlers.stream().filter(h -> Arrays.asList(h.getParameterTypes()).contains(exception)).findFirst();
            if (optional.isEmpty()) {
                Assertions.fail("Exception " + exception.getName() + " does not have a corresponding handle method in the endpoint");
            }
            final Method method = optional.get();
            /* exception */
            Assertions.assertNotNull(exception.getDeclaredAnnotation(ResponseStatus.class).code());
            Assertions.assertNotEquals(exception.getDeclaredAnnotation(ResponseStatus.class).code(), HttpStatus.INTERNAL_SERVER_ERROR);
            Assertions.assertNotNull(exception.getDeclaredAnnotation(ResponseStatus.class).reason(), "Exception " + exception.getName() + " does not provide a reason code");
            Assertions.assertTrue(errorCodes.contains(exception.getDeclaredAnnotation(ResponseStatus.class).reason()), "Exception code " + exception.getDeclaredAnnotation(ResponseStatus.class).reason() + " does have a reason code mapped in localized ui error messages");
            /* handler method */
            Assertions.assertEquals(method.getDeclaredAnnotation(ResponseStatus.class).code(), exception.getDeclaredAnnotation(ResponseStatus.class).code());
        }
    }
}
