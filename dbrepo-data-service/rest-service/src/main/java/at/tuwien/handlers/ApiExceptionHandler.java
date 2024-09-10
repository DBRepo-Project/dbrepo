package at.tuwien.handlers;

import at.tuwien.api.error.ApiErrorDto;
import at.tuwien.exception.*;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Log4j2
@ControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @Hidden
    @ResponseStatus(code = HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorDto> handle(AccessDeniedException e) {
        final HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/problem+json");
        final ApiErrorDto response = ApiErrorDto.builder()
                .code("error.access.denied")
                .message(e.getLocalizedMessage())
                .status(HttpStatus.UNAUTHORIZED)
                .build();
        return new ResponseEntity<>(response, headers, HttpStatus.UNAUTHORIZED);
    }

    @Hidden
    @ResponseStatus(code = HttpStatus.NOT_FOUND)
    @ExceptionHandler(AccessNotFoundException.class)
    public ResponseEntity<ApiErrorDto> handle(AccessNotFoundException e) {
        return generic_handle(e.getClass(), e.getLocalizedMessage());
    }

    @Hidden
    @ResponseStatus(code = HttpStatus.PRECONDITION_REQUIRED)
    @ExceptionHandler(AccountNotSetupException.class)
    public ResponseEntity<ApiErrorDto> handle(AccountNotSetupException e) {
        return generic_handle(e.getClass(), e.getLocalizedMessage());
    }

    @Hidden
    @ResponseStatus(code = HttpStatus.BAD_GATEWAY)
    @ExceptionHandler(AuthServiceConnectionException.class)
    public ResponseEntity<ApiErrorDto> handle(AuthServiceConnectionException e) {
        return generic_handle(e.getClass(), e.getLocalizedMessage());
    }

    @Hidden
    @ResponseStatus(code = HttpStatus.SERVICE_UNAVAILABLE)
    @ExceptionHandler(AuthServiceException.class)
    public ResponseEntity<ApiErrorDto> handle(AuthServiceException e) {
        return generic_handle(e.getClass(), e.getLocalizedMessage());
    }

    @Hidden
    @ResponseStatus(code = HttpStatus.BAD_GATEWAY)
    @ExceptionHandler(BrokerServiceConnectionException.class)
    public ResponseEntity<ApiErrorDto> handle(BrokerServiceConnectionException e) {
        return generic_handle(e.getClass(), e.getLocalizedMessage());
    }

    @Hidden
    @ResponseStatus(code = HttpStatus.SERVICE_UNAVAILABLE)
    @ExceptionHandler(BrokerServiceException.class)
    public ResponseEntity<ApiErrorDto> handle(BrokerServiceException e) {
        return generic_handle(e.getClass(), e.getLocalizedMessage());
    }

    @Hidden
    @ResponseStatus(code = HttpStatus.NOT_FOUND)
    @ExceptionHandler(ConceptNotFoundException.class)
    public ResponseEntity<ApiErrorDto> handle(ConceptNotFoundException e) {
        return generic_handle(e.getClass(), e.getLocalizedMessage());
    }

    @Hidden
    @ResponseStatus(code = HttpStatus.CONFLICT)
    @ExceptionHandler(ContainerAlreadyExistsException.class)
    public ResponseEntity<ApiErrorDto> handle(ContainerAlreadyExistsException e) {
        return generic_handle(e.getClass(), e.getLocalizedMessage());
    }

    @Hidden
    @ResponseStatus(code = HttpStatus.NOT_FOUND)
    @ExceptionHandler(ContainerNotFoundException.class)
    public ResponseEntity<ApiErrorDto> handle(ContainerNotFoundException e) {
        return generic_handle(e.getClass(), e.getLocalizedMessage());
    }

    @Hidden
    @ResponseStatus(code = HttpStatus.FORBIDDEN)
    @ExceptionHandler(CredentialsInvalidException.class)
    public ResponseEntity<ApiErrorDto> handle(CredentialsInvalidException e) {
        return generic_handle(e.getClass(), e.getLocalizedMessage());
    }

    @Hidden
    @ResponseStatus(code = HttpStatus.EXPECTATION_FAILED)
    @ExceptionHandler(DatabaseMalformedException.class)
    public ResponseEntity<ApiErrorDto> handle(DatabaseMalformedException e) {
        return generic_handle(e.getClass(), e.getLocalizedMessage());
    }

    @Hidden
    @ResponseStatus(code = HttpStatus.NOT_FOUND)
    @ExceptionHandler(DatabaseNotFoundException.class)
    public ResponseEntity<ApiErrorDto> handle(DatabaseNotFoundException e) {
        return generic_handle(e.getClass(), e.getLocalizedMessage());
    }

    @Hidden
    @ResponseStatus(code = HttpStatus.SERVICE_UNAVAILABLE)
    @ExceptionHandler(DatabaseUnavailableException.class)
    public ResponseEntity<ApiErrorDto> handle(DatabaseUnavailableException e) {
        return generic_handle(e.getClass(), e.getLocalizedMessage());
    }

    @Hidden
    @ResponseStatus(code = HttpStatus.NOT_FOUND)
    @ExceptionHandler(DoiNotFoundException.class)
    public ResponseEntity<ApiErrorDto> handle(DoiNotFoundException e) {
        return generic_handle(e.getClass(), e.getLocalizedMessage());
    }

    @Hidden
    @ResponseStatus(code = HttpStatus.EXPECTATION_FAILED)
    @ExceptionHandler(EmailExistsException.class)
    public ResponseEntity<ApiErrorDto> handle(EmailExistsException e) {
        return generic_handle(e.getClass(), e.getLocalizedMessage());
    }

    @Hidden
    @ResponseStatus(code = HttpStatus.NOT_FOUND)
    @ExceptionHandler(ExchangeNotFoundException.class)
    public ResponseEntity<ApiErrorDto> handle(ExchangeNotFoundException e) {
        return generic_handle(e.getClass(), e.getLocalizedMessage());
    }

    @Hidden
    @ResponseStatus(code = HttpStatus.SERVICE_UNAVAILABLE)
    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ApiErrorDto> handle(ExternalServiceException e) {
        return generic_handle(e.getClass(), e.getLocalizedMessage());
    }

    @Hidden
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    @ExceptionHandler(FilterBadRequestException.class)
    public ResponseEntity<ApiErrorDto> handle(FilterBadRequestException e) {
        return generic_handle(e.getClass(), e.getLocalizedMessage());
    }

    @Hidden
    @ResponseStatus(code = HttpStatus.NOT_ACCEPTABLE)
    @ExceptionHandler(FormatNotAvailableException.class)
    public ResponseEntity<ApiErrorDto> handle(FormatNotAvailableException e) {
        return generic_handle(e.getClass(), e.getLocalizedMessage());
    }

    @Hidden
    @ResponseStatus(code = HttpStatus.NOT_FOUND)
    @ExceptionHandler(IdentifierNotFoundException.class)
    public ResponseEntity<ApiErrorDto> handle(IdentifierNotFoundException e) {
        return generic_handle(e.getClass(), e.getLocalizedMessage());
    }

    @Hidden
    @ResponseStatus(code = HttpStatus.NOT_FOUND)
    @ExceptionHandler(IdentifierNotSupportedException.class)
    public ResponseEntity<ApiErrorDto> handle(IdentifierNotSupportedException e) {
        return generic_handle(e.getClass(), e.getLocalizedMessage());
    }

    @Hidden
    @ResponseStatus(code = HttpStatus.CONFLICT)
    @ExceptionHandler(ImageAlreadyExistsException.class)
    public ResponseEntity<ApiErrorDto> handle(ImageAlreadyExistsException e) {
        return generic_handle(e.getClass(), e.getLocalizedMessage());
    }

    @Hidden
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ImageInvalidException.class)
    public ResponseEntity<ApiErrorDto> handle(ImageInvalidException e) {
        return generic_handle(e.getClass(), e.getLocalizedMessage());
    }

    @Hidden
    @ResponseStatus(code = HttpStatus.NOT_FOUND)
    @ExceptionHandler(ImageNotFoundException.class)
    public ResponseEntity<ApiErrorDto> handle(ImageNotFoundException e) {
        return generic_handle(e.getClass(), e.getLocalizedMessage());
    }

    @Hidden
    @ResponseStatus(code = HttpStatus.NOT_FOUND)
    @ExceptionHandler(LicenseNotFoundException.class)
    public ResponseEntity<ApiErrorDto> handle(LicenseNotFoundException e) {
        return generic_handle(e.getClass(), e.getLocalizedMessage());
    }

    @Hidden
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MalformedException.class)
    public ResponseEntity<ApiErrorDto> handle(MalformedException e) {
        return generic_handle(e.getClass(), e.getLocalizedMessage());
    }

    @Hidden
    @ResponseStatus(code = HttpStatus.NOT_FOUND)
    @ExceptionHandler(MessageNotFoundException.class)
    public ResponseEntity<ApiErrorDto> handle(MessageNotFoundException e) {
        return generic_handle(e.getClass(), e.getLocalizedMessage());
    }

    @Hidden
    @ResponseStatus(code = HttpStatus.BAD_GATEWAY)
    @ExceptionHandler(MetadataServiceConnectionException.class)
    public ResponseEntity<ApiErrorDto> handle(MetadataServiceConnectionException e) {
        return generic_handle(e.getClass(), e.getLocalizedMessage());
    }

    @Hidden
    @ResponseStatus(code = HttpStatus.SERVICE_UNAVAILABLE)
    @ExceptionHandler(MetadataServiceException.class)
    public ResponseEntity<ApiErrorDto> handle(MetadataServiceException e) {
        return generic_handle(e.getClass(), e.getLocalizedMessage());
    }

    @Hidden
    @ResponseStatus(code = HttpStatus.FORBIDDEN)
    @ExceptionHandler(NotAllowedException.class)
    public ResponseEntity<ApiErrorDto> handle(NotAllowedException e) {
        return generic_handle(e.getClass(), e.getLocalizedMessage());
    }

    @Hidden
    @ResponseStatus(code = HttpStatus.NOT_FOUND)
    @ExceptionHandler(OntologyNotFoundException.class)
    public ResponseEntity<ApiErrorDto> handle(OntologyNotFoundException e) {
        return generic_handle(e.getClass(), e.getLocalizedMessage());
    }

    @Hidden
    @ResponseStatus(code = HttpStatus.NOT_FOUND)
    @ExceptionHandler(OrcidNotFoundException.class)
    public ResponseEntity<ApiErrorDto> handle(OrcidNotFoundException e) {
        return generic_handle(e.getClass(), e.getLocalizedMessage());
    }

    @Hidden
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    @ExceptionHandler(PaginationException.class)
    public ResponseEntity<ApiErrorDto> handle(PaginationException e) {
        return generic_handle(e.getClass(), e.getLocalizedMessage());
    }

    @Hidden
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    @ExceptionHandler(QueryMalformedException.class)
    public ResponseEntity<ApiErrorDto> handle(QueryMalformedException e) {
        return generic_handle(e.getClass(), e.getLocalizedMessage());
    }

    @Hidden
    @ResponseStatus(code = HttpStatus.NOT_FOUND)
    @ExceptionHandler(QueryNotFoundException.class)
    public ResponseEntity<ApiErrorDto> handle(QueryNotFoundException e) {
        return generic_handle(e.getClass(), e.getLocalizedMessage());
    }

    @Hidden
    @ResponseStatus(code = HttpStatus.NOT_IMPLEMENTED)
    @ExceptionHandler(QueryNotSupportedException.class)
    public ResponseEntity<ApiErrorDto> handle(QueryNotSupportedException e) {
        return generic_handle(e.getClass(), e.getLocalizedMessage());
    }

    @Hidden
    @ResponseStatus(code = HttpStatus.NOT_FOUND)
    @ExceptionHandler(QueueNotFoundException.class)
    public ResponseEntity<ApiErrorDto> handle(QueueNotFoundException e) {
        return generic_handle(e.getClass(), e.getLocalizedMessage());
    }

    @Hidden
    @ResponseStatus(code = HttpStatus.EXPECTATION_FAILED)
    @ExceptionHandler(QueryStoreCreateException.class)
    public ResponseEntity<ApiErrorDto> handle(QueryStoreCreateException e) {
        return generic_handle(e.getClass(), e.getLocalizedMessage());
    }

    @Hidden
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    @ExceptionHandler(QueryStoreGCException.class)
    public ResponseEntity<ApiErrorDto> handle(QueryStoreGCException e) {
        return generic_handle(e.getClass(), e.getLocalizedMessage());
    }

    @Hidden
    @ResponseStatus(code = HttpStatus.EXPECTATION_FAILED)
    @ExceptionHandler(QueryStoreInsertException.class)
    public ResponseEntity<ApiErrorDto> handle(QueryStoreInsertException e) {
        return generic_handle(e.getClass(), e.getLocalizedMessage());
    }

    @Hidden
    @ResponseStatus(code = HttpStatus.EXPECTATION_FAILED)
    @ExceptionHandler(QueryStorePersistException.class)
    public ResponseEntity<ApiErrorDto> handle(QueryStorePersistException e) {
        return generic_handle(e.getClass(), e.getLocalizedMessage());
    }

    @Hidden
    @ResponseStatus(code = HttpStatus.SERVICE_UNAVAILABLE)
    @ExceptionHandler(RemoteUnavailableException.class)
    public ResponseEntity<ApiErrorDto> handle(RemoteUnavailableException e) {
        return generic_handle(e.getClass(), e.getLocalizedMessage());
    }

    @Hidden
    @ResponseStatus(code = HttpStatus.NOT_FOUND)
    @ExceptionHandler(RorNotFoundException.class)
    public ResponseEntity<ApiErrorDto> handle(RorNotFoundException e) {
        return generic_handle(e.getClass(), e.getLocalizedMessage());
    }

    @Hidden
    @ResponseStatus(code = HttpStatus.BAD_GATEWAY)
    @ExceptionHandler(SearchServiceConnectionException.class)
    public ResponseEntity<ApiErrorDto> handle(SearchServiceConnectionException e) {
        return generic_handle(e.getClass(), e.getLocalizedMessage());
    }

    @Hidden
    @ResponseStatus(code = HttpStatus.SERVICE_UNAVAILABLE)
    @ExceptionHandler(SearchServiceException.class)
    public ResponseEntity<ApiErrorDto> handle(SearchServiceException e) {
        return generic_handle(e.getClass(), e.getLocalizedMessage());
    }

    @Hidden
    @ResponseStatus(code = HttpStatus.NOT_FOUND)
    @ExceptionHandler(SemanticEntityNotFoundException.class)
    public ResponseEntity<ApiErrorDto> handle(SemanticEntityNotFoundException e) {
        return generic_handle(e.getClass(), e.getLocalizedMessage());
    }

    @Hidden
    @ResponseStatus(code = HttpStatus.BAD_GATEWAY)
    @ExceptionHandler(DataServiceConnectionException.class)
    public ResponseEntity<ApiErrorDto> handle(DataServiceConnectionException e) {
        return generic_handle(e.getClass(), e.getLocalizedMessage());
    }

    @Hidden
    @ResponseStatus(code = HttpStatus.SERVICE_UNAVAILABLE)
    @ExceptionHandler(DataServiceException.class)
    public ResponseEntity<ApiErrorDto> handle(DataServiceException e) {
        return generic_handle(e.getClass(), e.getLocalizedMessage());
    }

    @Hidden
    @ResponseStatus(code = HttpStatus.SERVICE_UNAVAILABLE)
    @ExceptionHandler(SidecarExportException.class)
    public ResponseEntity<ApiErrorDto> handle(SidecarExportException e) {
        return generic_handle(e.getClass(), e.getLocalizedMessage());
    }

    @Hidden
    @ResponseStatus(code = HttpStatus.SERVICE_UNAVAILABLE)
    @ExceptionHandler(SidecarImportException.class)
    public ResponseEntity<ApiErrorDto> handle(SidecarImportException e) {
        return generic_handle(e.getClass(), e.getLocalizedMessage());
    }

    @Hidden
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    @ExceptionHandler(SortException.class)
    public ResponseEntity<ApiErrorDto> handle(SortException e) {
        return generic_handle(e.getClass(), e.getLocalizedMessage());
    }

    @Hidden
    @ResponseStatus(code = HttpStatus.NOT_FOUND)
    @ExceptionHandler(StorageNotFoundException.class)
    public ResponseEntity<ApiErrorDto> handle(StorageNotFoundException e) {
        return generic_handle(e.getClass(), e.getLocalizedMessage());
    }

    @Hidden
    @ResponseStatus(code = HttpStatus.SERVICE_UNAVAILABLE)
    @ExceptionHandler(StorageUnavailableException.class)
    public ResponseEntity<ApiErrorDto> handle(StorageUnavailableException e) {
        return generic_handle(e.getClass(), e.getLocalizedMessage());
    }

    @Hidden
    @ResponseStatus(code = HttpStatus.CONFLICT)
    @ExceptionHandler(TableExistsException.class)
    public ResponseEntity<ApiErrorDto> handle(TableExistsException e) {
        return generic_handle(e.getClass(), e.getLocalizedMessage());
    }

    @Hidden
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    @ExceptionHandler(TableMalformedException.class)
    public ResponseEntity<ApiErrorDto> handle(TableMalformedException e) {
        return generic_handle(e.getClass(), e.getLocalizedMessage());
    }

    @Hidden
    @ResponseStatus(code = HttpStatus.CONFLICT)
    @ExceptionHandler(TableSchemaException.class)
    public ResponseEntity<ApiErrorDto> handle(TableSchemaException e) {
        return generic_handle(e.getClass(), e.getLocalizedMessage());
    }

    @Hidden
    @ResponseStatus(code = HttpStatus.NOT_FOUND)
    @ExceptionHandler(TableNotFoundException.class)
    public ResponseEntity<ApiErrorDto> handle(TableNotFoundException e) {
        return generic_handle(e.getClass(), e.getLocalizedMessage());
    }

    @Hidden
    @ResponseStatus(code = HttpStatus.NOT_FOUND)
    @ExceptionHandler(UnitNotFoundException.class)
    public ResponseEntity<ApiErrorDto> handle(UnitNotFoundException e) {
        return generic_handle(e.getClass(), e.getLocalizedMessage());
    }

    @Hidden
    @ResponseStatus(code = HttpStatus.EXPECTATION_FAILED)
    @ExceptionHandler(UriMalformedException.class)
    public ResponseEntity<ApiErrorDto> handle(UriMalformedException e) {
        return generic_handle(e.getClass(), e.getLocalizedMessage());
    }

    @Hidden
    @ResponseStatus(code = HttpStatus.CONFLICT)
    @ExceptionHandler(UserExistsException.class)
    public ResponseEntity<ApiErrorDto> handle(UserExistsException e) {
        return generic_handle(e.getClass(), e.getLocalizedMessage());
    }

    @Hidden
    @ResponseStatus(code = HttpStatus.NOT_FOUND)
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiErrorDto> handle(UserNotFoundException e) {
        return generic_handle(e.getClass(), e.getLocalizedMessage());
    }

    @Hidden
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ViewMalformedException.class)
    public ResponseEntity<ApiErrorDto> handle(ViewMalformedException e) {
        return generic_handle(e.getClass(), e.getLocalizedMessage());
    }

    @Hidden
    @ResponseStatus(code = HttpStatus.NOT_FOUND)
    @ExceptionHandler(ViewNotFoundException.class)
    public ResponseEntity<ApiErrorDto> handle(ViewNotFoundException e) {
        return generic_handle(e.getClass(), e.getLocalizedMessage());
    }

    @Hidden
    @ResponseStatus(code = HttpStatus.CONFLICT)
    @ExceptionHandler(ViewSchemaException.class)
    public ResponseEntity<ApiErrorDto> handle(ViewSchemaException e) {
        return generic_handle(e.getClass(), e.getLocalizedMessage());
    }

    public ResponseEntity<ApiErrorDto> generic_handle(Class<?> exceptionClass, String message) {
        final HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/problem+json");
        final ResponseStatus annotation = exceptionClass.getAnnotation(ResponseStatus.class);
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(annotation.code())
                .message(message)
                .code(annotation.reason())
                .build();
        return new ResponseEntity<>(response, headers, response.getStatus());
    }

}
