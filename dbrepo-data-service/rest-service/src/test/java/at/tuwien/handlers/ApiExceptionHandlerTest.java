package at.tuwien.handlers;

import at.tuwien.api.error.ApiErrorDto;
import at.tuwien.exception.*;
import at.tuwien.test.AbstractUnitTest;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static at.tuwien.test.utils.EndpointUtils.getErrorCodes;
import static at.tuwien.test.utils.EndpointUtils.getExceptions;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Log4j2
@ExtendWith(SpringExtension.class)
@SpringBootTest
public class ApiExceptionHandlerTest extends AbstractUnitTest {

    @Autowired
    private ApiExceptionHandler apiExceptionHandler;

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
            assertNotNull(exception.getDeclaredAnnotation(ResponseStatus.class).code());
            Assertions.assertNotEquals(exception.getDeclaredAnnotation(ResponseStatus.class).code(), HttpStatus.INTERNAL_SERVER_ERROR);
            assertNotNull(exception.getDeclaredAnnotation(ResponseStatus.class).reason(), "Exception " + exception.getName() + " does not provide a reason code");
            Assertions.assertTrue(errorCodes.contains(exception.getDeclaredAnnotation(ResponseStatus.class).reason()), "Exception code " + exception.getDeclaredAnnotation(ResponseStatus.class).reason() + " does have a reason code mapped in localized ui error messages");
            /* handler method */
            Assertions.assertEquals(method.getDeclaredAnnotation(ResponseStatus.class).code(), exception.getDeclaredAnnotation(ResponseStatus.class).code());
        }
    }

    @Test
    public void generic_handle_succeeds() {

        /* test */
        apiExceptionHandler.generic_handle(DatabaseNotFoundException.class, "msg");
    }

    @Test
    public void handle_AccessDeniedException_succeeds() {
        final AccessDeniedException request = new AccessDeniedException("msg");

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(request);
        assertNotNull(response);
        Assertions.assertNotEquals(response.getStatusCode(), HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("error.access.denied", response.getBody().getCode());
        assertEquals("msg", response.getBody().getMessage());
    }


    @Test
    public void handle_AccessNotFoundException_succeeds() {
        final AccessNotFoundException request = new AccessNotFoundException("msg");

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(request);
        assertNotNull(response);
        assertNotNull(request.getClass().getDeclaredAnnotation(ResponseStatus.class).code());
        final HttpStatus httpStatus = request.getClass().getDeclaredAnnotation(ResponseStatus.class).code();
        Assertions.assertNotEquals(httpStatus, HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals(httpStatus, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getCode());
        assertEquals(httpStatus, response.getBody().getStatus());
        assertEquals("msg", response.getBody().getMessage());
    }


    @Test
    public void handle_AccountNotSetupException_succeeds() {
        final AccountNotSetupException request = new AccountNotSetupException("msg");

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(request);
        assertNotNull(response);
        assertNotNull(request.getClass().getDeclaredAnnotation(ResponseStatus.class).code());
        final HttpStatus httpStatus = request.getClass().getDeclaredAnnotation(ResponseStatus.class).code();
        Assertions.assertNotEquals(httpStatus, HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals(httpStatus, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getCode());
        assertEquals(httpStatus, response.getBody().getStatus());
        assertEquals("msg", response.getBody().getMessage());
    }


    @Test
    public void handle_AuthServiceConnectionException_succeeds() {
        final AuthServiceConnectionException request = new AuthServiceConnectionException("msg");

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(request);
        assertNotNull(response);
        assertNotNull(request.getClass().getDeclaredAnnotation(ResponseStatus.class).code());
        final HttpStatus httpStatus = request.getClass().getDeclaredAnnotation(ResponseStatus.class).code();
        Assertions.assertNotEquals(httpStatus, HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals(httpStatus, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getCode());
        assertEquals(httpStatus, response.getBody().getStatus());
        assertEquals("msg", response.getBody().getMessage());
    }


    @Test
    public void handle_AuthServiceException_succeeds() {
        final AuthServiceException request = new AuthServiceException("msg");

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(request);
        assertNotNull(response);
        assertNotNull(request.getClass().getDeclaredAnnotation(ResponseStatus.class).code());
        final HttpStatus httpStatus = request.getClass().getDeclaredAnnotation(ResponseStatus.class).code();
        Assertions.assertNotEquals(httpStatus, HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals(httpStatus, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getCode());
        assertEquals(httpStatus, response.getBody().getStatus());
        assertEquals("msg", response.getBody().getMessage());
    }


    @Test
    public void handle_BrokerServiceConnectionException_succeeds() {
        final BrokerServiceConnectionException request = new BrokerServiceConnectionException("msg");

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(request);
        assertNotNull(response);
        assertNotNull(request.getClass().getDeclaredAnnotation(ResponseStatus.class).code());
        final HttpStatus httpStatus = request.getClass().getDeclaredAnnotation(ResponseStatus.class).code();
        Assertions.assertNotEquals(httpStatus, HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals(httpStatus, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getCode());
        assertEquals(httpStatus, response.getBody().getStatus());
        assertEquals("msg", response.getBody().getMessage());
    }


    @Test
    public void handle_BrokerServiceException_succeeds() {
        final BrokerServiceException request = new BrokerServiceException("msg");

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(request);
        assertNotNull(response);
        assertNotNull(request.getClass().getDeclaredAnnotation(ResponseStatus.class).code());
        final HttpStatus httpStatus = request.getClass().getDeclaredAnnotation(ResponseStatus.class).code();
        Assertions.assertNotEquals(httpStatus, HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals(httpStatus, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getCode());
        assertEquals(httpStatus, response.getBody().getStatus());
        assertEquals("msg", response.getBody().getMessage());
    }


    @Test
    public void handle_ConceptNotFoundException_succeeds() {
        final ConceptNotFoundException request = new ConceptNotFoundException("msg");

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(request);
        assertNotNull(response);
        assertNotNull(request.getClass().getDeclaredAnnotation(ResponseStatus.class).code());
        final HttpStatus httpStatus = request.getClass().getDeclaredAnnotation(ResponseStatus.class).code();
        Assertions.assertNotEquals(httpStatus, HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals(httpStatus, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getCode());
        assertEquals(httpStatus, response.getBody().getStatus());
        assertEquals("msg", response.getBody().getMessage());
    }


    @Test
    public void handle_ContainerAlreadyExistsException_succeeds() {
        final ContainerAlreadyExistsException request = new ContainerAlreadyExistsException("msg");

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(request);
        assertNotNull(response);
        assertNotNull(request.getClass().getDeclaredAnnotation(ResponseStatus.class).code());
        final HttpStatus httpStatus = request.getClass().getDeclaredAnnotation(ResponseStatus.class).code();
        Assertions.assertNotEquals(httpStatus, HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals(httpStatus, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getCode());
        assertEquals(httpStatus, response.getBody().getStatus());
        assertEquals("msg", response.getBody().getMessage());
    }


    @Test
    public void handle_ContainerNotFoundException_succeeds() {
        final ContainerNotFoundException request = new ContainerNotFoundException("msg");

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(request);
        assertNotNull(response);
        assertNotNull(request.getClass().getDeclaredAnnotation(ResponseStatus.class).code());
        final HttpStatus httpStatus = request.getClass().getDeclaredAnnotation(ResponseStatus.class).code();
        Assertions.assertNotEquals(httpStatus, HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals(httpStatus, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getCode());
        assertEquals(httpStatus, response.getBody().getStatus());
        assertEquals("msg", response.getBody().getMessage());
    }


    @Test
    public void handle_CredentialsInvalidException_succeeds() {
        final CredentialsInvalidException request = new CredentialsInvalidException("msg");

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(request);
        assertNotNull(response);
        assertNotNull(request.getClass().getDeclaredAnnotation(ResponseStatus.class).code());
        final HttpStatus httpStatus = request.getClass().getDeclaredAnnotation(ResponseStatus.class).code();
        Assertions.assertNotEquals(httpStatus, HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals(httpStatus, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getCode());
        assertEquals(httpStatus, response.getBody().getStatus());
        assertEquals("msg", response.getBody().getMessage());
    }


    @Test
    public void handle_DatabaseMalformedException_succeeds() {
        final DatabaseMalformedException request = new DatabaseMalformedException("msg");

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(request);
        assertNotNull(response);
        assertNotNull(request.getClass().getDeclaredAnnotation(ResponseStatus.class).code());
        final HttpStatus httpStatus = request.getClass().getDeclaredAnnotation(ResponseStatus.class).code();
        Assertions.assertNotEquals(httpStatus, HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals(httpStatus, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getCode());
        assertEquals(httpStatus, response.getBody().getStatus());
        assertEquals("msg", response.getBody().getMessage());
    }


    @Test
    public void handle_DatabaseNotFoundException_succeeds() {
        final DatabaseNotFoundException request = new DatabaseNotFoundException("msg");

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(request);
        assertNotNull(response);
        assertNotNull(request.getClass().getDeclaredAnnotation(ResponseStatus.class).code());
        final HttpStatus httpStatus = request.getClass().getDeclaredAnnotation(ResponseStatus.class).code();
        Assertions.assertNotEquals(httpStatus, HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals(httpStatus, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getCode());
        assertEquals(httpStatus, response.getBody().getStatus());
        assertEquals("msg", response.getBody().getMessage());
    }


    @Test
    public void handle_DatabaseUnavailableException_succeeds() {
        final DatabaseUnavailableException request = new DatabaseUnavailableException("msg");

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(request);
        assertNotNull(response);
        assertNotNull(request.getClass().getDeclaredAnnotation(ResponseStatus.class).code());
        final HttpStatus httpStatus = request.getClass().getDeclaredAnnotation(ResponseStatus.class).code();
        Assertions.assertNotEquals(httpStatus, HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals(httpStatus, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getCode());
        assertEquals(httpStatus, response.getBody().getStatus());
        assertEquals("msg", response.getBody().getMessage());
    }


    @Test
    public void handle_DoiNotFoundException_succeeds() {
        final DoiNotFoundException request = new DoiNotFoundException("msg");

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(request);
        assertNotNull(response);
        assertNotNull(request.getClass().getDeclaredAnnotation(ResponseStatus.class).code());
        final HttpStatus httpStatus = request.getClass().getDeclaredAnnotation(ResponseStatus.class).code();
        Assertions.assertNotEquals(httpStatus, HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals(httpStatus, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getCode());
        assertEquals(httpStatus, response.getBody().getStatus());
        assertEquals("msg", response.getBody().getMessage());
    }


    @Test
    public void handle_EmailExistsException_succeeds() {
        final EmailExistsException request = new EmailExistsException("msg");

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(request);
        assertNotNull(response);
        assertNotNull(request.getClass().getDeclaredAnnotation(ResponseStatus.class).code());
        final HttpStatus httpStatus = request.getClass().getDeclaredAnnotation(ResponseStatus.class).code();
        Assertions.assertNotEquals(httpStatus, HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals(httpStatus, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getCode());
        assertEquals(httpStatus, response.getBody().getStatus());
        assertEquals("msg", response.getBody().getMessage());
    }


    @Test
    public void handle_ExchangeNotFoundException_succeeds() {
        final ExchangeNotFoundException request = new ExchangeNotFoundException("msg");

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(request);
        assertNotNull(response);
        assertNotNull(request.getClass().getDeclaredAnnotation(ResponseStatus.class).code());
        final HttpStatus httpStatus = request.getClass().getDeclaredAnnotation(ResponseStatus.class).code();
        Assertions.assertNotEquals(httpStatus, HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals(httpStatus, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getCode());
        assertEquals(httpStatus, response.getBody().getStatus());
        assertEquals("msg", response.getBody().getMessage());
    }


    @Test
    public void handle_ExternalServiceException_succeeds() {
        final ExternalServiceException request = new ExternalServiceException("msg");

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(request);
        assertNotNull(response);
        assertNotNull(request.getClass().getDeclaredAnnotation(ResponseStatus.class).code());
        final HttpStatus httpStatus = request.getClass().getDeclaredAnnotation(ResponseStatus.class).code();
        Assertions.assertNotEquals(httpStatus, HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals(httpStatus, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getCode());
        assertEquals(httpStatus, response.getBody().getStatus());
        assertEquals("msg", response.getBody().getMessage());
    }


    @Test
    public void handle_FilterBadRequestException_succeeds() {
        final FilterBadRequestException request = new FilterBadRequestException("msg");

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(request);
        assertNotNull(response);
        assertNotNull(request.getClass().getDeclaredAnnotation(ResponseStatus.class).code());
        final HttpStatus httpStatus = request.getClass().getDeclaredAnnotation(ResponseStatus.class).code();
        Assertions.assertNotEquals(httpStatus, HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals(httpStatus, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getCode());
        assertEquals(httpStatus, response.getBody().getStatus());
        assertEquals("msg", response.getBody().getMessage());
    }


    @Test
    public void handle_FormatNotAvailableException_succeeds() {
        final FormatNotAvailableException request = new FormatNotAvailableException("msg");

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(request);
        assertNotNull(response);
        assertNotNull(request.getClass().getDeclaredAnnotation(ResponseStatus.class).code());
        final HttpStatus httpStatus = request.getClass().getDeclaredAnnotation(ResponseStatus.class).code();
        Assertions.assertNotEquals(httpStatus, HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals(httpStatus, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getCode());
        assertEquals(httpStatus, response.getBody().getStatus());
        assertEquals("msg", response.getBody().getMessage());
    }


    @Test
    public void handle_IdentifierNotFoundException_succeeds() {
        final IdentifierNotFoundException request = new IdentifierNotFoundException("msg");

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(request);
        assertNotNull(response);
        assertNotNull(request.getClass().getDeclaredAnnotation(ResponseStatus.class).code());
        final HttpStatus httpStatus = request.getClass().getDeclaredAnnotation(ResponseStatus.class).code();
        Assertions.assertNotEquals(httpStatus, HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals(httpStatus, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getCode());
        assertEquals(httpStatus, response.getBody().getStatus());
        assertEquals("msg", response.getBody().getMessage());
    }


    @Test
    public void handle_IdentifierNotSupportedException_succeeds() {
        final IdentifierNotSupportedException request = new IdentifierNotSupportedException("msg");

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(request);
        assertNotNull(response);
        assertNotNull(request.getClass().getDeclaredAnnotation(ResponseStatus.class).code());
        final HttpStatus httpStatus = request.getClass().getDeclaredAnnotation(ResponseStatus.class).code();
        Assertions.assertNotEquals(httpStatus, HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals(httpStatus, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getCode());
        assertEquals(httpStatus, response.getBody().getStatus());
        assertEquals("msg", response.getBody().getMessage());
    }


    @Test
    public void handle_ImageAlreadyExistsException_succeeds() {
        final ImageAlreadyExistsException request = new ImageAlreadyExistsException("msg");

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(request);
        assertNotNull(response);
        assertNotNull(request.getClass().getDeclaredAnnotation(ResponseStatus.class).code());
        final HttpStatus httpStatus = request.getClass().getDeclaredAnnotation(ResponseStatus.class).code();
        Assertions.assertNotEquals(httpStatus, HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals(httpStatus, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getCode());
        assertEquals(httpStatus, response.getBody().getStatus());
        assertEquals("msg", response.getBody().getMessage());
    }


    @Test
    public void handle_ImageInvalidException_succeeds() {
        final ImageInvalidException request = new ImageInvalidException("msg");

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(request);
        assertNotNull(response);
        assertNotNull(request.getClass().getDeclaredAnnotation(ResponseStatus.class).code());
        final HttpStatus httpStatus = request.getClass().getDeclaredAnnotation(ResponseStatus.class).code();
        Assertions.assertNotEquals(httpStatus, HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals(httpStatus, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getCode());
        assertEquals(httpStatus, response.getBody().getStatus());
        assertEquals("msg", response.getBody().getMessage());
    }


    @Test
    public void handle_ImageNotFoundException_succeeds() {
        final ImageNotFoundException request = new ImageNotFoundException("msg");

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(request);
        assertNotNull(response);
        assertNotNull(request.getClass().getDeclaredAnnotation(ResponseStatus.class).code());
        final HttpStatus httpStatus = request.getClass().getDeclaredAnnotation(ResponseStatus.class).code();
        Assertions.assertNotEquals(httpStatus, HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals(httpStatus, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getCode());
        assertEquals(httpStatus, response.getBody().getStatus());
        assertEquals("msg", response.getBody().getMessage());
    }


    @Test
    public void handle_LicenseNotFoundException_succeeds() {
        final LicenseNotFoundException request = new LicenseNotFoundException("msg");

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(request);
        assertNotNull(response);
        assertNotNull(request.getClass().getDeclaredAnnotation(ResponseStatus.class).code());
        final HttpStatus httpStatus = request.getClass().getDeclaredAnnotation(ResponseStatus.class).code();
        Assertions.assertNotEquals(httpStatus, HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals(httpStatus, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getCode());
        assertEquals(httpStatus, response.getBody().getStatus());
        assertEquals("msg", response.getBody().getMessage());
    }


    @Test
    public void handle_MalformedException_succeeds() {
        final MalformedException request = new MalformedException("msg");

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(request);
        assertNotNull(response);
        assertNotNull(request.getClass().getDeclaredAnnotation(ResponseStatus.class).code());
        final HttpStatus httpStatus = request.getClass().getDeclaredAnnotation(ResponseStatus.class).code();
        Assertions.assertNotEquals(httpStatus, HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals(httpStatus, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getCode());
        assertEquals(httpStatus, response.getBody().getStatus());
        assertEquals("msg", response.getBody().getMessage());
    }


    @Test
    public void handle_MessageNotFoundException_succeeds() {
        final MessageNotFoundException request = new MessageNotFoundException("msg");

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(request);
        assertNotNull(response);
        assertNotNull(request.getClass().getDeclaredAnnotation(ResponseStatus.class).code());
        final HttpStatus httpStatus = request.getClass().getDeclaredAnnotation(ResponseStatus.class).code();
        Assertions.assertNotEquals(httpStatus, HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals(httpStatus, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getCode());
        assertEquals(httpStatus, response.getBody().getStatus());
        assertEquals("msg", response.getBody().getMessage());
    }


    @Test
    public void handle_MetadataServiceConnectionException_succeeds() {
        final MetadataServiceConnectionException request = new MetadataServiceConnectionException("msg");

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(request);
        assertNotNull(response);
        assertNotNull(request.getClass().getDeclaredAnnotation(ResponseStatus.class).code());
        final HttpStatus httpStatus = request.getClass().getDeclaredAnnotation(ResponseStatus.class).code();
        Assertions.assertNotEquals(httpStatus, HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals(httpStatus, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getCode());
        assertEquals(httpStatus, response.getBody().getStatus());
        assertEquals("msg", response.getBody().getMessage());
    }


    @Test
    public void handle_MetadataServiceException_succeeds() {
        final MetadataServiceException request = new MetadataServiceException("msg");

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(request);
        assertNotNull(response);
        assertNotNull(request.getClass().getDeclaredAnnotation(ResponseStatus.class).code());
        final HttpStatus httpStatus = request.getClass().getDeclaredAnnotation(ResponseStatus.class).code();
        Assertions.assertNotEquals(httpStatus, HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals(httpStatus, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getCode());
        assertEquals(httpStatus, response.getBody().getStatus());
        assertEquals("msg", response.getBody().getMessage());
    }


    @Test
    public void handle_NotAllowedException_succeeds() {
        final NotAllowedException request = new NotAllowedException("msg");

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(request);
        assertNotNull(response);
        assertNotNull(request.getClass().getDeclaredAnnotation(ResponseStatus.class).code());
        final HttpStatus httpStatus = request.getClass().getDeclaredAnnotation(ResponseStatus.class).code();
        Assertions.assertNotEquals(httpStatus, HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals(httpStatus, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getCode());
        assertEquals(httpStatus, response.getBody().getStatus());
        assertEquals("msg", response.getBody().getMessage());
    }


    @Test
    public void handle_OntologyNotFoundException_succeeds() {
        final OntologyNotFoundException request = new OntologyNotFoundException("msg");

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(request);
        assertNotNull(response);
        assertNotNull(request.getClass().getDeclaredAnnotation(ResponseStatus.class).code());
        final HttpStatus httpStatus = request.getClass().getDeclaredAnnotation(ResponseStatus.class).code();
        Assertions.assertNotEquals(httpStatus, HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals(httpStatus, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getCode());
        assertEquals(httpStatus, response.getBody().getStatus());
        assertEquals("msg", response.getBody().getMessage());
    }


    @Test
    public void handle_OrcidNotFoundException_succeeds() {
        final OrcidNotFoundException request = new OrcidNotFoundException("msg");

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(request);
        assertNotNull(response);
        assertNotNull(request.getClass().getDeclaredAnnotation(ResponseStatus.class).code());
        final HttpStatus httpStatus = request.getClass().getDeclaredAnnotation(ResponseStatus.class).code();
        Assertions.assertNotEquals(httpStatus, HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals(httpStatus, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getCode());
        assertEquals(httpStatus, response.getBody().getStatus());
        assertEquals("msg", response.getBody().getMessage());
    }


    @Test
    public void handle_PaginationException_succeeds() {
        final PaginationException request = new PaginationException("msg");

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(request);
        assertNotNull(response);
        assertNotNull(request.getClass().getDeclaredAnnotation(ResponseStatus.class).code());
        final HttpStatus httpStatus = request.getClass().getDeclaredAnnotation(ResponseStatus.class).code();
        Assertions.assertNotEquals(httpStatus, HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals(httpStatus, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getCode());
        assertEquals(httpStatus, response.getBody().getStatus());
        assertEquals("msg", response.getBody().getMessage());
    }


    @Test
    public void handle_QueryMalformedException_succeeds() {
        final QueryMalformedException request = new QueryMalformedException("msg");

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(request);
        assertNotNull(response);
        assertNotNull(request.getClass().getDeclaredAnnotation(ResponseStatus.class).code());
        final HttpStatus httpStatus = request.getClass().getDeclaredAnnotation(ResponseStatus.class).code();
        Assertions.assertNotEquals(httpStatus, HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals(httpStatus, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getCode());
        assertEquals(httpStatus, response.getBody().getStatus());
        assertEquals("msg", response.getBody().getMessage());
    }


    @Test
    public void handle_QueryNotFoundException_succeeds() {
        final QueryNotFoundException request = new QueryNotFoundException("msg");

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(request);
        assertNotNull(response);
        assertNotNull(request.getClass().getDeclaredAnnotation(ResponseStatus.class).code());
        final HttpStatus httpStatus = request.getClass().getDeclaredAnnotation(ResponseStatus.class).code();
        Assertions.assertNotEquals(httpStatus, HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals(httpStatus, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getCode());
        assertEquals(httpStatus, response.getBody().getStatus());
        assertEquals("msg", response.getBody().getMessage());
    }


    @Test
    public void handle_QueryNotSupportedException_succeeds() {
        final QueryNotSupportedException request = new QueryNotSupportedException("msg");

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(request);
        assertNotNull(response);
        assertNotNull(request.getClass().getDeclaredAnnotation(ResponseStatus.class).code());
        final HttpStatus httpStatus = request.getClass().getDeclaredAnnotation(ResponseStatus.class).code();
        Assertions.assertNotEquals(httpStatus, HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals(httpStatus, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getCode());
        assertEquals(httpStatus, response.getBody().getStatus());
        assertEquals("msg", response.getBody().getMessage());
    }


    @Test
    public void handle_QueueNotFoundException_succeeds() {
        final QueueNotFoundException request = new QueueNotFoundException("msg");

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(request);
        assertNotNull(response);
        assertNotNull(request.getClass().getDeclaredAnnotation(ResponseStatus.class).code());
        final HttpStatus httpStatus = request.getClass().getDeclaredAnnotation(ResponseStatus.class).code();
        Assertions.assertNotEquals(httpStatus, HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals(httpStatus, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getCode());
        assertEquals(httpStatus, response.getBody().getStatus());
        assertEquals("msg", response.getBody().getMessage());
    }


    @Test
    public void handle_QueryStoreCreateException_succeeds() {
        final QueryStoreCreateException request = new QueryStoreCreateException("msg");

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(request);
        assertNotNull(response);
        assertNotNull(request.getClass().getDeclaredAnnotation(ResponseStatus.class).code());
        final HttpStatus httpStatus = request.getClass().getDeclaredAnnotation(ResponseStatus.class).code();
        Assertions.assertNotEquals(httpStatus, HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals(httpStatus, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getCode());
        assertEquals(httpStatus, response.getBody().getStatus());
        assertEquals("msg", response.getBody().getMessage());
    }


    @Test
    public void handle_QueryStoreGCException_succeeds() {
        final QueryStoreGCException request = new QueryStoreGCException("msg");

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(request);
        assertNotNull(response);
        assertNotNull(request.getClass().getDeclaredAnnotation(ResponseStatus.class).code());
        final HttpStatus httpStatus = request.getClass().getDeclaredAnnotation(ResponseStatus.class).code();
        Assertions.assertNotEquals(httpStatus, HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals(httpStatus, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getCode());
        assertEquals(httpStatus, response.getBody().getStatus());
        assertEquals("msg", response.getBody().getMessage());
    }


    @Test
    public void handle_QueryStoreInsertException_succeeds() {
        final QueryStoreInsertException request = new QueryStoreInsertException("msg");

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(request);
        assertNotNull(response);
        assertNotNull(request.getClass().getDeclaredAnnotation(ResponseStatus.class).code());
        final HttpStatus httpStatus = request.getClass().getDeclaredAnnotation(ResponseStatus.class).code();
        Assertions.assertNotEquals(httpStatus, HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals(httpStatus, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getCode());
        assertEquals(httpStatus, response.getBody().getStatus());
        assertEquals("msg", response.getBody().getMessage());
    }


    @Test
    public void handle_QueryStorePersistException_succeeds() {
        final QueryStorePersistException request = new QueryStorePersistException("msg");

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(request);
        assertNotNull(response);
        assertNotNull(request.getClass().getDeclaredAnnotation(ResponseStatus.class).code());
        final HttpStatus httpStatus = request.getClass().getDeclaredAnnotation(ResponseStatus.class).code();
        Assertions.assertNotEquals(httpStatus, HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals(httpStatus, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getCode());
        assertEquals(httpStatus, response.getBody().getStatus());
        assertEquals("msg", response.getBody().getMessage());
    }


    @Test
    public void handle_RemoteUnavailableException_succeeds() {
        final RemoteUnavailableException request = new RemoteUnavailableException("msg");

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(request);
        assertNotNull(response);
        assertNotNull(request.getClass().getDeclaredAnnotation(ResponseStatus.class).code());
        final HttpStatus httpStatus = request.getClass().getDeclaredAnnotation(ResponseStatus.class).code();
        Assertions.assertNotEquals(httpStatus, HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals(httpStatus, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getCode());
        assertEquals(httpStatus, response.getBody().getStatus());
        assertEquals("msg", response.getBody().getMessage());
    }


    @Test
    public void handle_RorNotFoundException_succeeds() {
        final RorNotFoundException request = new RorNotFoundException("msg");

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(request);
        assertNotNull(response);
        assertNotNull(request.getClass().getDeclaredAnnotation(ResponseStatus.class).code());
        final HttpStatus httpStatus = request.getClass().getDeclaredAnnotation(ResponseStatus.class).code();
        Assertions.assertNotEquals(httpStatus, HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals(httpStatus, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getCode());
        assertEquals(httpStatus, response.getBody().getStatus());
        assertEquals("msg", response.getBody().getMessage());
    }


    @Test
    public void handle_SearchServiceConnectionException_succeeds() {
        final SearchServiceConnectionException request = new SearchServiceConnectionException("msg");

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(request);
        assertNotNull(response);
        assertNotNull(request.getClass().getDeclaredAnnotation(ResponseStatus.class).code());
        final HttpStatus httpStatus = request.getClass().getDeclaredAnnotation(ResponseStatus.class).code();
        Assertions.assertNotEquals(httpStatus, HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals(httpStatus, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getCode());
        assertEquals(httpStatus, response.getBody().getStatus());
        assertEquals("msg", response.getBody().getMessage());
    }


    @Test
    public void handle_SearchServiceException_succeeds() {
        final SearchServiceException request = new SearchServiceException("msg");

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(request);
        assertNotNull(response);
        assertNotNull(request.getClass().getDeclaredAnnotation(ResponseStatus.class).code());
        final HttpStatus httpStatus = request.getClass().getDeclaredAnnotation(ResponseStatus.class).code();
        Assertions.assertNotEquals(httpStatus, HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals(httpStatus, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getCode());
        assertEquals(httpStatus, response.getBody().getStatus());
        assertEquals("msg", response.getBody().getMessage());
    }


    @Test
    public void handle_SemanticEntityNotFoundException_succeeds() {
        final SemanticEntityNotFoundException request = new SemanticEntityNotFoundException("msg");

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(request);
        assertNotNull(response);
        assertNotNull(request.getClass().getDeclaredAnnotation(ResponseStatus.class).code());
        final HttpStatus httpStatus = request.getClass().getDeclaredAnnotation(ResponseStatus.class).code();
        Assertions.assertNotEquals(httpStatus, HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals(httpStatus, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getCode());
        assertEquals(httpStatus, response.getBody().getStatus());
        assertEquals("msg", response.getBody().getMessage());
    }


    @Test
    public void handle_DataServiceConnectionException_succeeds() {
        final DataServiceConnectionException request = new DataServiceConnectionException("msg");

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(request);
        assertNotNull(response);
        assertNotNull(request.getClass().getDeclaredAnnotation(ResponseStatus.class).code());
        final HttpStatus httpStatus = request.getClass().getDeclaredAnnotation(ResponseStatus.class).code();
        Assertions.assertNotEquals(httpStatus, HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals(httpStatus, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getCode());
        assertEquals(httpStatus, response.getBody().getStatus());
        assertEquals("msg", response.getBody().getMessage());
    }


    @Test
    public void handle_DataServiceException_succeeds() {
        final DataServiceException request = new DataServiceException("msg");

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(request);
        assertNotNull(response);
        assertNotNull(request.getClass().getDeclaredAnnotation(ResponseStatus.class).code());
        final HttpStatus httpStatus = request.getClass().getDeclaredAnnotation(ResponseStatus.class).code();
        Assertions.assertNotEquals(httpStatus, HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals(httpStatus, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getCode());
        assertEquals(httpStatus, response.getBody().getStatus());
        assertEquals("msg", response.getBody().getMessage());
    }


    @Test
    public void handle_SidecarExportException_succeeds() {
        final SidecarExportException request = new SidecarExportException("msg");

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(request);
        assertNotNull(response);
        assertNotNull(request.getClass().getDeclaredAnnotation(ResponseStatus.class).code());
        final HttpStatus httpStatus = request.getClass().getDeclaredAnnotation(ResponseStatus.class).code();
        Assertions.assertNotEquals(httpStatus, HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals(httpStatus, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getCode());
        assertEquals(httpStatus, response.getBody().getStatus());
        assertEquals("msg", response.getBody().getMessage());
    }


    @Test
    public void handle_SidecarImportException_succeeds() {
        final SidecarImportException request = new SidecarImportException("msg");

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(request);
        assertNotNull(response);
        assertNotNull(request.getClass().getDeclaredAnnotation(ResponseStatus.class).code());
        final HttpStatus httpStatus = request.getClass().getDeclaredAnnotation(ResponseStatus.class).code();
        Assertions.assertNotEquals(httpStatus, HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals(httpStatus, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getCode());
        assertEquals(httpStatus, response.getBody().getStatus());
        assertEquals("msg", response.getBody().getMessage());
    }


    @Test
    public void handle_SortException_succeeds() {
        final SortException request = new SortException("msg");

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(request);
        assertNotNull(response);
        assertNotNull(request.getClass().getDeclaredAnnotation(ResponseStatus.class).code());
        final HttpStatus httpStatus = request.getClass().getDeclaredAnnotation(ResponseStatus.class).code();
        Assertions.assertNotEquals(httpStatus, HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals(httpStatus, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getCode());
        assertEquals(httpStatus, response.getBody().getStatus());
        assertEquals("msg", response.getBody().getMessage());
    }


    @Test
    public void handle_StorageNotFoundException_succeeds() {
        final StorageNotFoundException request = new StorageNotFoundException("msg");

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(request);
        assertNotNull(response);
        assertNotNull(request.getClass().getDeclaredAnnotation(ResponseStatus.class).code());
        final HttpStatus httpStatus = request.getClass().getDeclaredAnnotation(ResponseStatus.class).code();
        Assertions.assertNotEquals(httpStatus, HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals(httpStatus, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getCode());
        assertEquals(httpStatus, response.getBody().getStatus());
        assertEquals("msg", response.getBody().getMessage());
    }


    @Test
    public void handle_StorageUnavailableException_succeeds() {
        final StorageUnavailableException request = new StorageUnavailableException("msg");

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(request);
        assertNotNull(response);
        assertNotNull(request.getClass().getDeclaredAnnotation(ResponseStatus.class).code());
        final HttpStatus httpStatus = request.getClass().getDeclaredAnnotation(ResponseStatus.class).code();
        Assertions.assertNotEquals(httpStatus, HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals(httpStatus, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getCode());
        assertEquals(httpStatus, response.getBody().getStatus());
        assertEquals("msg", response.getBody().getMessage());
    }


    @Test
    public void handle_TableExistsException_succeeds() {
        final TableExistsException request = new TableExistsException("msg");

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(request);
        assertNotNull(response);
        assertNotNull(request.getClass().getDeclaredAnnotation(ResponseStatus.class).code());
        final HttpStatus httpStatus = request.getClass().getDeclaredAnnotation(ResponseStatus.class).code();
        Assertions.assertNotEquals(httpStatus, HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals(httpStatus, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getCode());
        assertEquals(httpStatus, response.getBody().getStatus());
        assertEquals("msg", response.getBody().getMessage());
    }


    @Test
    public void handle_TableMalformedException_succeeds() {
        final TableMalformedException request = new TableMalformedException("msg");

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(request);
        assertNotNull(response);
        assertNotNull(request.getClass().getDeclaredAnnotation(ResponseStatus.class).code());
        final HttpStatus httpStatus = request.getClass().getDeclaredAnnotation(ResponseStatus.class).code();
        Assertions.assertNotEquals(httpStatus, HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals(httpStatus, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getCode());
        assertEquals(httpStatus, response.getBody().getStatus());
        assertEquals("msg", response.getBody().getMessage());
    }


    @Test
    public void handle_TableSchemaException_succeeds() {
        final TableSchemaException request = new TableSchemaException("msg");

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(request);
        assertNotNull(response);
        assertNotNull(request.getClass().getDeclaredAnnotation(ResponseStatus.class).code());
        final HttpStatus httpStatus = request.getClass().getDeclaredAnnotation(ResponseStatus.class).code();
        Assertions.assertNotEquals(httpStatus, HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals(httpStatus, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getCode());
        assertEquals(httpStatus, response.getBody().getStatus());
        assertEquals("msg", response.getBody().getMessage());
    }


    @Test
    public void handle_TableNotFoundException_succeeds() {
        final TableNotFoundException request = new TableNotFoundException("msg");

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(request);
        assertNotNull(response);
        assertNotNull(request.getClass().getDeclaredAnnotation(ResponseStatus.class).code());
        final HttpStatus httpStatus = request.getClass().getDeclaredAnnotation(ResponseStatus.class).code();
        Assertions.assertNotEquals(httpStatus, HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals(httpStatus, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getCode());
        assertEquals(httpStatus, response.getBody().getStatus());
        assertEquals("msg", response.getBody().getMessage());
    }


    @Test
    public void handle_UnitNotFoundException_succeeds() {
        final UnitNotFoundException request = new UnitNotFoundException("msg");

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(request);
        assertNotNull(response);
        assertNotNull(request.getClass().getDeclaredAnnotation(ResponseStatus.class).code());
        final HttpStatus httpStatus = request.getClass().getDeclaredAnnotation(ResponseStatus.class).code();
        Assertions.assertNotEquals(httpStatus, HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals(httpStatus, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getCode());
        assertEquals(httpStatus, response.getBody().getStatus());
        assertEquals("msg", response.getBody().getMessage());
    }


    @Test
    public void handle_UriMalformedException_succeeds() {
        final UriMalformedException request = new UriMalformedException("msg");

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(request);
        assertNotNull(response);
        assertNotNull(request.getClass().getDeclaredAnnotation(ResponseStatus.class).code());
        final HttpStatus httpStatus = request.getClass().getDeclaredAnnotation(ResponseStatus.class).code();
        Assertions.assertNotEquals(httpStatus, HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals(httpStatus, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getCode());
        assertEquals(httpStatus, response.getBody().getStatus());
        assertEquals("msg", response.getBody().getMessage());
    }


    @Test
    public void handle_UserExistsException_succeeds() {
        final UserExistsException request = new UserExistsException("msg");

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(request);
        assertNotNull(response);
        assertNotNull(request.getClass().getDeclaredAnnotation(ResponseStatus.class).code());
        final HttpStatus httpStatus = request.getClass().getDeclaredAnnotation(ResponseStatus.class).code();
        Assertions.assertNotEquals(httpStatus, HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals(httpStatus, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getCode());
        assertEquals(httpStatus, response.getBody().getStatus());
        assertEquals("msg", response.getBody().getMessage());
    }


    @Test
    public void handle_UserNotFoundException_succeeds() {
        final UserNotFoundException request = new UserNotFoundException("msg");

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(request);
        assertNotNull(response);
        assertNotNull(request.getClass().getDeclaredAnnotation(ResponseStatus.class).code());
        final HttpStatus httpStatus = request.getClass().getDeclaredAnnotation(ResponseStatus.class).code();
        Assertions.assertNotEquals(httpStatus, HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals(httpStatus, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getCode());
        assertEquals(httpStatus, response.getBody().getStatus());
        assertEquals("msg", response.getBody().getMessage());
    }


    @Test
    public void handle_ViewMalformedException_succeeds() {
        final ViewMalformedException request = new ViewMalformedException("msg");

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(request);
        assertNotNull(response);
        assertNotNull(request.getClass().getDeclaredAnnotation(ResponseStatus.class).code());
        final HttpStatus httpStatus = request.getClass().getDeclaredAnnotation(ResponseStatus.class).code();
        Assertions.assertNotEquals(httpStatus, HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals(httpStatus, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getCode());
        assertEquals(httpStatus, response.getBody().getStatus());
        assertEquals("msg", response.getBody().getMessage());
    }


    @Test
    public void handle_ViewNotFoundException_succeeds() {
        final ViewNotFoundException request = new ViewNotFoundException("msg");

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(request);
        assertNotNull(response);
        assertNotNull(request.getClass().getDeclaredAnnotation(ResponseStatus.class).code());
        final HttpStatus httpStatus = request.getClass().getDeclaredAnnotation(ResponseStatus.class).code();
        Assertions.assertNotEquals(httpStatus, HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals(httpStatus, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getCode());
        assertEquals(httpStatus, response.getBody().getStatus());
        assertEquals("msg", response.getBody().getMessage());
    }


    @Test
    public void handle_ViewSchemaException_succeeds() {
        final ViewSchemaException request = new ViewSchemaException("msg");

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(request);
        assertNotNull(response);
        assertNotNull(request.getClass().getDeclaredAnnotation(ResponseStatus.class).code());
        final HttpStatus httpStatus = request.getClass().getDeclaredAnnotation(ResponseStatus.class).code();
        Assertions.assertNotEquals(httpStatus, HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals(httpStatus, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getCode());
        assertEquals(httpStatus, response.getBody().getStatus());
        assertEquals("msg", response.getBody().getMessage());
    }

}
