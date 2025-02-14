package at.tuwien.handlers;

import at.tuwien.api.error.ApiErrorDto;
import at.tuwien.exception.*;
import at.tuwien.test.AbstractUnitTest;
import com.auth0.jwt.exceptions.TokenExpiredException;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.IOException;
import java.lang.reflect.Method;
import java.time.Instant;
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
            Assertions.assertNotNull(exception.getDeclaredAnnotation(ResponseStatus.class).code());
            Assertions.assertNotEquals(exception.getDeclaredAnnotation(ResponseStatus.class).code(), HttpStatus.INTERNAL_SERVER_ERROR);
            Assertions.assertNotNull(exception.getDeclaredAnnotation(ResponseStatus.class).reason(), "Exception " + exception.getName() + " does not provide a reason code");
            Assertions.assertTrue(errorCodes.contains(exception.getDeclaredAnnotation(ResponseStatus.class).reason()), "Exception code " + exception.getDeclaredAnnotation(ResponseStatus.class).reason() + " does have a reason code mapped in localized ui error messages");
            /* handler method */
            assertEquals(method.getDeclaredAnnotation(ResponseStatus.class).code(), exception.getDeclaredAnnotation(ResponseStatus.class).code());
        }
    }

    @Test
    public void handle_tokenExpiredException_succeeds() {

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(new TokenExpiredException("msg", Instant.now()));
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        final ApiErrorDto body = response.getBody();
        assertNotNull(body);
        assertNotNull(body.getMessage());
        assertEquals(HttpStatus.UNAUTHORIZED, body.getStatus());
        assertEquals("error.token.expired", body.getCode());
    }

    @Test
    public void handle_accessNotFoundException_succeeds() {

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(new AccessNotFoundException("msg"));
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        final ApiErrorDto body = response.getBody();
        assertNotNull(body);
        assertEquals("msg", body.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, body.getStatus());
        assertEquals("error.access.missing", body.getCode());
    }

    @Test
    public void handle_accountNotSetupException_succeeds() {

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(new AccountNotSetupException("msg"));
        assertEquals(HttpStatus.PRECONDITION_REQUIRED, response.getStatusCode());
        final ApiErrorDto body = response.getBody();
        assertNotNull(body);
        assertEquals("msg", body.getMessage());
        assertEquals(HttpStatus.PRECONDITION_REQUIRED, body.getStatus());
        assertEquals("error.user.setup", body.getCode());
    }

    @Test
    public void handle_analyseServiceException_succeeds() {

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(new AnalyseServiceException("msg"));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        final ApiErrorDto body = response.getBody();
        assertNotNull(body);
        assertEquals("msg", body.getMessage());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, body.getStatus());
        assertEquals("error.analyse.invalid", body.getCode());
    }

    @Test
    public void handle_authServiceConnectionException_succeeds() {

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(new AuthServiceConnectionException("msg"));
        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
        final ApiErrorDto body = response.getBody();
        assertNotNull(body);
        assertEquals("msg", body.getMessage());
        assertEquals(HttpStatus.BAD_GATEWAY, body.getStatus());
        assertEquals("error.auth.connection", body.getCode());
    }

    @Test
    public void handle_authServiceException_succeeds() {

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(new AuthServiceException("msg"));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        final ApiErrorDto body = response.getBody();
        assertNotNull(body);
        assertEquals("msg", body.getMessage());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, body.getStatus());
        assertEquals("error.auth.invalid", body.getCode());
    }


    @Test
    public void handle_brokerServiceConnectionException_succeeds() {

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(new BrokerServiceConnectionException("msg"));
        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
        final ApiErrorDto body = response.getBody();
        assertNotNull(body);
        assertEquals("msg", body.getMessage());
        assertEquals(HttpStatus.BAD_GATEWAY, body.getStatus());
        assertEquals("error.broker.connection", body.getCode());
    }


    @Test
    public void handle_brokerServiceException_succeeds() {

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(new BrokerServiceException("msg"));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        final ApiErrorDto body = response.getBody();
        assertNotNull(body);
        assertEquals("msg", body.getMessage());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, body.getStatus());
        assertEquals("error.broker.invalid", body.getCode());
    }


    @Test
    public void handle_conceptNotFoundException_succeeds() {

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(new ConceptNotFoundException("msg"));
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        final ApiErrorDto body = response.getBody();
        assertNotNull(body);
        assertEquals("msg", body.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, body.getStatus());
        assertEquals("error.concept.missing", body.getCode());
    }


    @Test
    public void handle_containerAlreadyExistsException_succeeds() {

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(new ContainerAlreadyExistsException("msg"));
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        final ApiErrorDto body = response.getBody();
        assertNotNull(body);
        assertEquals("msg", body.getMessage());
        assertEquals(HttpStatus.CONFLICT, body.getStatus());
        assertEquals("error.container.exists", body.getCode());
    }


    @Test
    public void handle_containerNotFoundException_succeeds() {

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(new ContainerNotFoundException("msg"));
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        final ApiErrorDto body = response.getBody();
        assertNotNull(body);
        assertEquals("msg", body.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, body.getStatus());
        assertEquals("error.container.missing", body.getCode());
    }


    @Test
    public void handle_containerQuotaException_succeeds() {

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(new ContainerQuotaException("msg"));
        assertEquals(HttpStatus.LOCKED, response.getStatusCode());
        final ApiErrorDto body = response.getBody();
        assertNotNull(body);
        assertEquals("msg", body.getMessage());
        assertEquals(HttpStatus.LOCKED, body.getStatus());
        assertEquals("error.container.quota", body.getCode());
    }


    @Test
    public void handle_credentialsInvalidException_succeeds() {

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(new CredentialsInvalidException("msg"));
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        final ApiErrorDto body = response.getBody();
        assertNotNull(body);
        assertEquals("msg", body.getMessage());
        assertEquals(HttpStatus.FORBIDDEN, body.getStatus());
        assertEquals("error.user.credentials", body.getCode());
    }


    @Test
    public void handle_dataServiceConnectionException_succeeds() {

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(new DataServiceConnectionException("msg"));
        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
        final ApiErrorDto body = response.getBody();
        assertNotNull(body);
        assertEquals("msg", body.getMessage());
        assertEquals(HttpStatus.BAD_GATEWAY, body.getStatus());
        assertEquals("error.data.connection", body.getCode());
    }


    @Test
    public void handle_dataServiceException_succeeds() {

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(new DataServiceException("msg"));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        final ApiErrorDto body = response.getBody();
        assertNotNull(body);
        assertEquals("msg", body.getMessage());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, body.getStatus());
        assertEquals("error.data.invalid", body.getCode());
    }


    @Test
    public void handle_databaseMalformedException_succeeds() {

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(new DatabaseMalformedException("msg"));
        assertEquals(HttpStatus.EXPECTATION_FAILED, response.getStatusCode());
        final ApiErrorDto body = response.getBody();
        assertNotNull(body);
        assertEquals("msg", body.getMessage());
        assertEquals(HttpStatus.EXPECTATION_FAILED, body.getStatus());
        assertEquals("error.database.invalid", body.getCode());
    }


    @Test
    public void handle_databaseNotFoundException_succeeds() {

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(new DatabaseNotFoundException("msg"));
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        final ApiErrorDto body = response.getBody();
        assertNotNull(body);
        assertEquals("msg", body.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, body.getStatus());
        assertEquals("error.database.missing", body.getCode());
    }


    @Test
    public void handle_databaseUnavailableException_succeeds() {

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(new DatabaseUnavailableException("msg"));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        final ApiErrorDto body = response.getBody();
        assertNotNull(body);
        assertEquals("msg", body.getMessage());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, body.getStatus());
        assertEquals("error.database.connection", body.getCode());
    }


    @Test
    public void handle_doiNotFoundException_succeeds() {

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(new DoiNotFoundException("msg"));
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        final ApiErrorDto body = response.getBody();
        assertNotNull(body);
        assertEquals("msg", body.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, body.getStatus());
        assertEquals("error.doi.missing", body.getCode());
    }


    @Test
    public void handle_emailExistsException_succeeds() {

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(new EmailExistsException("msg"));
        assertEquals(HttpStatus.EXPECTATION_FAILED, response.getStatusCode());
        final ApiErrorDto body = response.getBody();
        assertNotNull(body);
        assertEquals("msg", body.getMessage());
        assertEquals(HttpStatus.EXPECTATION_FAILED, body.getStatus());
        assertEquals("error.user.email-exists", body.getCode());
    }


    @Test
    public void handle_exchangeNotFoundException_succeeds() {

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(new ExchangeNotFoundException("msg"));
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        final ApiErrorDto body = response.getBody();
        assertNotNull(body);
        assertEquals("msg", body.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, body.getStatus());
        assertEquals("error.exchange.missing", body.getCode());
    }


    @Test
    public void handle_externalServiceException_succeeds() {

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(new ExternalServiceException("msg"));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        final ApiErrorDto body = response.getBody();
        assertNotNull(body);
        assertEquals("msg", body.getMessage());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, body.getStatus());
        assertEquals("error.external.invalid", body.getCode());
    }


    @Test
    public void handle_filterBadRequestException_succeeds() {

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(new FilterBadRequestException("msg"));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        final ApiErrorDto body = response.getBody();
        assertNotNull(body);
        assertEquals("msg", body.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, body.getStatus());
        assertEquals("error.semantic.filter", body.getCode());
    }


    @Test
    public void handle_formatNotAvailableException_succeeds() {

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(new FormatNotAvailableException("msg"));
        assertEquals(HttpStatus.NOT_ACCEPTABLE, response.getStatusCode());
        final ApiErrorDto body = response.getBody();
        assertNotNull(body);
        assertEquals("msg", body.getMessage());
        assertEquals(HttpStatus.NOT_ACCEPTABLE, body.getStatus());
        assertEquals("error.identifier.format", body.getCode());
    }


    @Test
    public void handle_identifierNotFoundException_succeeds() {

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(new IdentifierNotFoundException("msg"));
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        final ApiErrorDto body = response.getBody();
        assertNotNull(body);
        assertEquals("msg", body.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, body.getStatus());
        assertEquals("error.identifier.missing", body.getCode());
    }


    @Test
    public void handle_identifierNotSupportedException_succeeds() {

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(new IdentifierNotSupportedException("msg"));
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        final ApiErrorDto body = response.getBody();
        assertNotNull(body);
        assertEquals("msg", body.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, body.getStatus());
        assertEquals("error.identifier.unsupported", body.getCode());
    }


    @Test
    public void handle_imageAlreadyExistsException_succeeds() {

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(new ImageAlreadyExistsException("msg"));
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        final ApiErrorDto body = response.getBody();
        assertNotNull(body);
        assertEquals("msg", body.getMessage());
        assertEquals(HttpStatus.CONFLICT, body.getStatus());
        assertEquals("error.image.exists", body.getCode());
    }


    @Test
    public void handle_imageInvalidException_succeeds() {

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(new ImageInvalidException("msg"));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        final ApiErrorDto body = response.getBody();
        assertNotNull(body);
        assertEquals("msg", body.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, body.getStatus());
        assertEquals("error.image.invalid", body.getCode());
    }


    @Test
    public void handle_imageNotFoundException_succeeds() {

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(new ImageNotFoundException("msg"));
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        final ApiErrorDto body = response.getBody();
        assertNotNull(body);
        assertEquals("msg", body.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, body.getStatus());
        assertEquals("error.image.missing", body.getCode());
    }


    @Test
    public void handle_licenseNotFoundException_succeeds() {

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(new LicenseNotFoundException("msg"));
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        final ApiErrorDto body = response.getBody();
        assertNotNull(body);
        assertEquals("msg", body.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, body.getStatus());
        assertEquals("error.license.missing", body.getCode());
    }


    @Test
    public void handle_malformedException_succeeds() {

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(new MalformedException("msg"));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        final ApiErrorDto body = response.getBody();
        assertNotNull(body);
        assertEquals("msg", body.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, body.getStatus());
        assertEquals("error.request.invalid", body.getCode());
    }


    @Test
    public void handle_messageNotFoundException_succeeds() {

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(new MessageNotFoundException("msg"));
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        final ApiErrorDto body = response.getBody();
        assertNotNull(body);
        assertEquals("msg", body.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, body.getStatus());
        assertEquals("error.message.missing", body.getCode());
    }


    @Test
    public void handle_metadataServiceConnectionException_succeeds() {

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(new MetadataServiceConnectionException("msg"));
        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
        final ApiErrorDto body = response.getBody();
        assertNotNull(body);
        assertEquals("msg", body.getMessage());
        assertEquals(HttpStatus.BAD_GATEWAY, body.getStatus());
        assertEquals("error.metadata.connection", body.getCode());
    }


    @Test
    public void handle_metadataServiceException_succeeds() {

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(new MetadataServiceException("msg"));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        final ApiErrorDto body = response.getBody();
        assertNotNull(body);
        assertEquals("msg", body.getMessage());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, body.getStatus());
        assertEquals("error.metadata.invalid", body.getCode());
    }


    @Test
    public void handle_notAllowedException_succeeds() {

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(new NotAllowedException("msg"));
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        final ApiErrorDto body = response.getBody();
        assertNotNull(body);
        assertEquals("msg", body.getMessage());
        assertEquals(HttpStatus.FORBIDDEN, body.getStatus());
        assertEquals("error.request.forbidden", body.getCode());
    }


    @Test
    public void handle_ontologyNotFoundException_succeeds() {

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(new OntologyNotFoundException("msg"));
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        final ApiErrorDto body = response.getBody();
        assertNotNull(body);
        assertEquals("msg", body.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, body.getStatus());
        assertEquals("error.ontology.missing", body.getCode());
    }


    @Test
    public void handle_orcidNotFoundException_succeeds() {

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(new OrcidNotFoundException("msg"));
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        final ApiErrorDto body = response.getBody();
        assertNotNull(body);
        assertEquals("msg", body.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, body.getStatus());
        assertEquals("error.orcid.missing", body.getCode());
    }


    @Test
    public void handle_paginationException_succeeds() {

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(new PaginationException("msg"));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        final ApiErrorDto body = response.getBody();
        assertNotNull(body);
        assertEquals("msg", body.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, body.getStatus());
        assertEquals("error.request.pagination", body.getCode());
    }


    @Test
    public void handle_queryMalformedException_succeeds() {

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(new QueryMalformedException("msg"));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        final ApiErrorDto body = response.getBody();
        assertNotNull(body);
        assertEquals("msg", body.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, body.getStatus());
        assertEquals("error.query.invalid", body.getCode());
    }


    @Test
    public void handle_queryNotFoundException_succeeds() {

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(new QueryNotFoundException("msg"));
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        final ApiErrorDto body = response.getBody();
        assertNotNull(body);
        assertEquals("msg", body.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, body.getStatus());
        assertEquals("error.query.missing", body.getCode());
    }


    @Test
    public void handle_queryNotSupportedException_succeeds() {

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(new QueryNotSupportedException("msg"));
        assertEquals(HttpStatus.NOT_IMPLEMENTED, response.getStatusCode());
        final ApiErrorDto body = response.getBody();
        assertNotNull(body);
        assertEquals("msg", body.getMessage());
        assertEquals(HttpStatus.NOT_IMPLEMENTED, body.getStatus());
        assertEquals("error.query.invalid", body.getCode());
    }


    @Test
    public void handle_queryStoreCreateException_succeeds() {

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(new QueryStoreCreateException("msg"));
        assertEquals(HttpStatus.EXPECTATION_FAILED, response.getStatusCode());
        final ApiErrorDto body = response.getBody();
        assertNotNull(body);
        assertEquals("msg", body.getMessage());
        assertEquals(HttpStatus.EXPECTATION_FAILED, body.getStatus());
        assertEquals("error.store.invalid", body.getCode());
    }


    @Test
    public void handle_queryStoreGCException_succeeds() {

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(new QueryStoreGCException("msg"));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        final ApiErrorDto body = response.getBody();
        assertNotNull(body);
        assertEquals("msg", body.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, body.getStatus());
        assertEquals("error.store.clean", body.getCode());
    }


    @Test
    public void handle_queryStoreInsertException_succeeds() {

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(new QueryStoreInsertException("msg"));
        assertEquals(HttpStatus.EXPECTATION_FAILED, response.getStatusCode());
        final ApiErrorDto body = response.getBody();
        assertNotNull(body);
        assertEquals("msg", body.getMessage());
        assertEquals(HttpStatus.EXPECTATION_FAILED, body.getStatus());
        assertEquals("error.store.insert", body.getCode());
    }


    @Test
    public void handle_queryStorePersistException_succeeds() {

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(new QueryStorePersistException("msg"));
        assertEquals(HttpStatus.EXPECTATION_FAILED, response.getStatusCode());
        final ApiErrorDto body = response.getBody();
        assertNotNull(body);
        assertEquals("msg", body.getMessage());
        assertEquals(HttpStatus.EXPECTATION_FAILED, body.getStatus());
        assertEquals("error.store.persist", body.getCode());
    }


    @Test
    public void handle_queueNotFoundException_succeeds() {

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(new QueueNotFoundException("msg"));
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        final ApiErrorDto body = response.getBody();
        assertNotNull(body);
        assertEquals("msg", body.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, body.getStatus());
        assertEquals("error.queue.missing", body.getCode());
    }


    @Test
    public void handle_remoteUnavailableException_succeeds() {

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(new RemoteUnavailableException("msg"));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        final ApiErrorDto body = response.getBody();
        assertNotNull(body);
        assertEquals("msg", body.getMessage());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, body.getStatus());
        assertEquals("error.metadata.privileged", body.getCode());
    }


    @Test
    public void handle_rorNotFoundException_succeeds() {

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(new RorNotFoundException("msg"));
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        final ApiErrorDto body = response.getBody();
        assertNotNull(body);
        assertEquals("msg", body.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, body.getStatus());
        assertEquals("error.ror.missing", body.getCode());
    }


    @Test
    public void handle_searchServiceConnectionException_succeeds() {

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(new SearchServiceConnectionException("msg"));
        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
        final ApiErrorDto body = response.getBody();
        assertNotNull(body);
        assertEquals("msg", body.getMessage());
        assertEquals(HttpStatus.BAD_GATEWAY, body.getStatus());
        assertEquals("error.search.connection", body.getCode());
    }


    @Test
    public void handle_searchServiceException_succeeds() {

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(new SearchServiceException("msg"));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        final ApiErrorDto body = response.getBody();
        assertNotNull(body);
        assertEquals("msg", body.getMessage());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, body.getStatus());
        assertEquals("error.search.invalid", body.getCode());
    }


    @Test
    public void handle_semanticEntityNotFoundException_succeeds() {

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(new SemanticEntityNotFoundException("msg"));
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        final ApiErrorDto body = response.getBody();
        assertNotNull(body);
        assertEquals("msg", body.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, body.getStatus());
        assertEquals("error.semantic.missing", body.getCode());
    }


    @Test
    public void handle_sortException_succeeds() {

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(new SortException("msg"));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        final ApiErrorDto body = response.getBody();
        assertNotNull(body);
        assertEquals("msg", body.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, body.getStatus());
        assertEquals("error.request.sort", body.getCode());
    }


    @Test
    public void handle_storageNotFoundException_succeeds() {

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(new StorageNotFoundException("msg"));
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        final ApiErrorDto body = response.getBody();
        assertNotNull(body);
        assertEquals("msg", body.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, body.getStatus());
        assertEquals("error.storage.missing", body.getCode());
    }


    @Test
    public void handle_storageUnavailableException_succeeds() {

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(new StorageUnavailableException("msg"));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        final ApiErrorDto body = response.getBody();
        assertNotNull(body);
        assertEquals("msg", body.getMessage());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, body.getStatus());
        assertEquals("error.storage.invalid", body.getCode());
    }


    @Test
    public void handle_tableExistsException_succeeds() {

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(new TableExistsException("msg"));
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        final ApiErrorDto body = response.getBody();
        assertNotNull(body);
        assertEquals("msg", body.getMessage());
        assertEquals(HttpStatus.CONFLICT, body.getStatus());
        assertEquals("error.table.exists", body.getCode());
    }


    @Test
    public void handle_tableMalformedException_succeeds() {

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(new TableMalformedException("msg"));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        final ApiErrorDto body = response.getBody();
        assertNotNull(body);
        assertEquals("msg", body.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, body.getStatus());
        assertEquals("error.table.invalid", body.getCode());
    }


    @Test
    public void handle_tableNotFoundException_succeeds() {

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(new TableNotFoundException("msg"));
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        final ApiErrorDto body = response.getBody();
        assertNotNull(body);
        assertEquals("msg", body.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, body.getStatus());
        assertEquals("error.table.missing", body.getCode());
    }


    @Test
    public void handle_tableSchemaException_succeeds() {

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(new TableSchemaException("msg"));
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        final ApiErrorDto body = response.getBody();
        assertNotNull(body);
        assertEquals("msg", body.getMessage());
        assertEquals(HttpStatus.CONFLICT, body.getStatus());
        assertEquals("error.schema.table", body.getCode());
    }


    @Test
    public void handle_unitNotFoundException_succeeds() {

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(new UnitNotFoundException("msg"));
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        final ApiErrorDto body = response.getBody();
        assertNotNull(body);
        assertEquals("msg", body.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, body.getStatus());
        assertEquals("error.unit.missing", body.getCode());
    }


    @Test
    public void handle_uriMalformedException_succeeds() {

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(new UriMalformedException("msg"));
        assertEquals(HttpStatus.EXPECTATION_FAILED, response.getStatusCode());
        final ApiErrorDto body = response.getBody();
        assertNotNull(body);
        assertEquals("msg", body.getMessage());
        assertEquals(HttpStatus.EXPECTATION_FAILED, body.getStatus());
        assertEquals("error.semantics.uri", body.getCode());
    }


    @Test
    public void handle_userExistsException_succeeds() {

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(new UserExistsException("msg"));
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        final ApiErrorDto body = response.getBody();
        assertNotNull(body);
        assertEquals("msg", body.getMessage());
        assertEquals(HttpStatus.CONFLICT, body.getStatus());
        assertEquals("error.user.exists", body.getCode());
    }


    @Test
    public void handle_userNotFoundException_succeeds() {

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(new UserNotFoundException("msg"));
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        final ApiErrorDto body = response.getBody();
        assertNotNull(body);
        assertEquals("msg", body.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, body.getStatus());
        assertEquals("error.user.missing", body.getCode());
    }


    @Test
    public void handle_viewMalformedException_succeeds() {

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(new ViewMalformedException("msg"));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        final ApiErrorDto body = response.getBody();
        assertNotNull(body);
        assertEquals("msg", body.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, body.getStatus());
        assertEquals("error.view.invalid", body.getCode());
    }


    @Test
    public void handle_viewNotFoundException_succeeds() {

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(new ViewNotFoundException("msg"));
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        final ApiErrorDto body = response.getBody();
        assertNotNull(body);
        assertEquals("msg", body.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, body.getStatus());
        assertEquals("error.view.missing", body.getCode());
    }


    @Test
    public void handle_viewSchemaException_succeeds() {

        /* test */
        final ResponseEntity<ApiErrorDto> response = apiExceptionHandler.handle(new ViewSchemaException("msg"));
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        final ApiErrorDto body = response.getBody();
        assertNotNull(body);
        assertEquals("msg", body.getMessage());
        assertEquals(HttpStatus.CONFLICT, body.getStatus());
        assertEquals("error.schema.view", body.getCode());
    }

}
