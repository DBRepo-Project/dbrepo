package at.tuwien.handlers;

import at.tuwien.api.error.ApiErrorDto;
import at.tuwien.exception.*;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Log4j2
@ControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static HttpHeaders headers(WebRequest webRequest) {
        final HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/problem+json");
        log.trace("setting response headers {}", headers);
        return headers;
    }

    @Hidden
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(ContainerNotFoundException.class)
    public ResponseEntity<ApiErrorDto> handle(ContainerNotFoundException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.NOT_FOUND)
                .message(e.getLocalizedMessage())
                .code("error.container.missing")
                .build();
        return new ResponseEntity<>(response, headers(request), response.getStatus());
    }

    @Hidden
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(DatabaseMalformedException.class)
    public ResponseEntity<ApiErrorDto> handle(DatabaseMalformedException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.BAD_REQUEST)
                .message(e.getLocalizedMessage())
                .code("error.database.invalid")
                .build();
        return new ResponseEntity<>(response, headers(request), response.getStatus());
    }

    @Hidden
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(DatabaseNotFoundException.class)
    public ResponseEntity<ApiErrorDto> handle(DatabaseNotFoundException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.NOT_FOUND)
                .message(e.getLocalizedMessage())
                .code("error.database.missing")
                .build();
        return new ResponseEntity<>(response, headers(request), response.getStatus());
    }

    @Hidden
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    @ExceptionHandler(DatabaseUnavailableException.class)
    public ResponseEntity<ApiErrorDto> handle(DatabaseUnavailableException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .message(e.getLocalizedMessage())
                .code("error.database.connection")
                .build();
        return new ResponseEntity<>(response, headers(request), response.getStatus());
    }

    @Hidden
    @ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
    @ExceptionHandler(FormatNotAvailableException.class)
    public ResponseEntity<ApiErrorDto> handle(FormatNotAvailableException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.NOT_ACCEPTABLE)
                .message(e.getLocalizedMessage())
                .code("error.subset.format")
                .build();
        return new ResponseEntity<>(response, headers(request), response.getStatus());
    }

    @Hidden
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(NotAllowedException.class)
    public ResponseEntity<ApiErrorDto> handle(NotAllowedException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.FORBIDDEN)
                .message(e.getLocalizedMessage())
                .code("error.request.forbidden")
                .build();
        return new ResponseEntity<>(response, headers(request), response.getStatus());
    }

    @Hidden
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(QueryMalformedException.class)
    public ResponseEntity<ApiErrorDto> handle(QueryMalformedException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.BAD_REQUEST)
                .message(e.getLocalizedMessage())
                .code("error.query.invalid")
                .build();
        return new ResponseEntity<>(response, headers(request), response.getStatus());
    }

    @Hidden
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(QueryNotFoundException.class)
    public ResponseEntity<ApiErrorDto> handle(QueryNotFoundException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.NOT_FOUND)
                .message(e.getLocalizedMessage())
                .code("error.query.missing")
                .build();
        return new ResponseEntity<>(response, headers(request), response.getStatus());
    }

    @Hidden
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(QueryStoreCreateException.class)
    public ResponseEntity<ApiErrorDto> handle(QueryStoreCreateException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.BAD_REQUEST)
                .message(e.getLocalizedMessage())
                .code("error.store.invalid")
                .build();
        return new ResponseEntity<>(response, headers(request), response.getStatus());
    }

    @Hidden
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(QueryStoreGCException.class)
    public ResponseEntity<ApiErrorDto> handle(QueryStoreGCException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.BAD_REQUEST)
                .message(e.getLocalizedMessage())
                .code("error.store.clean")
                .build();
        return new ResponseEntity<>(response, headers(request), response.getStatus());
    }

    @Hidden
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(QueryStoreInsertException.class)
    public ResponseEntity<ApiErrorDto> handle(QueryStoreInsertException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.BAD_REQUEST)
                .message(e.getLocalizedMessage())
                .code("error.store.insert")
                .build();
        return new ResponseEntity<>(response, headers(request), response.getStatus());
    }

    @Hidden
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(QueryStorePersistException.class)
    public ResponseEntity<ApiErrorDto> handle(QueryStorePersistException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.BAD_REQUEST)
                .message(e.getLocalizedMessage())
                .code("error.store.persist")
                .build();
        return new ResponseEntity<>(response, headers(request), response.getStatus());
    }

    @Hidden
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    @ExceptionHandler(RemoteUnavailableException.class)
    public ResponseEntity<ApiErrorDto> handle(RemoteUnavailableException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .message(e.getLocalizedMessage())
                .code("error.metadata.privileged")
                .build();
        return new ResponseEntity<>(response, headers(request), response.getStatus());
    }

    @Hidden
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    @ExceptionHandler(ServiceConnectionException.class)
    public ResponseEntity<ApiErrorDto> handle(ServiceConnectionException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.BAD_GATEWAY)
                .message(e.getLocalizedMessage())
                .code("error.metadata.connection")
                .build();
        return new ResponseEntity<>(response, headers(request), response.getStatus());
    }

    @Hidden
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<ApiErrorDto> handle(ServiceException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .message(e.getLocalizedMessage())
                .code("error.metadata.invalid")
                .build();
        return new ResponseEntity<>(response, headers(request), response.getStatus());
    }

    @Hidden
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    @ExceptionHandler(SidecarExportException.class)
    public ResponseEntity<ApiErrorDto> handle(SidecarExportException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .message(e.getLocalizedMessage())
                .code("error.sidecar.export")
                .build();
        return new ResponseEntity<>(response, headers(request), response.getStatus());
    }

    @Hidden
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    @ExceptionHandler(SidecarImportException.class)
    public ResponseEntity<ApiErrorDto> handle(SidecarImportException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .message(e.getLocalizedMessage())
                .code("error.sidecar.import")
                .build();
        return new ResponseEntity<>(response, headers(request), response.getStatus());
    }

    @Hidden
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(StorageNotFoundException.class)
    public ResponseEntity<ApiErrorDto> handle(StorageNotFoundException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.NOT_FOUND)
                .message(e.getLocalizedMessage())
                .code("error.storage.missing")
                .build();
        return new ResponseEntity<>(response, headers(request), response.getStatus());
    }

    @Hidden
    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(TableExistsException.class)
    public ResponseEntity<ApiErrorDto> handle(TableExistsException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.CONFLICT)
                .message(e.getLocalizedMessage())
                .code("error.table.exists")
                .build();
        return new ResponseEntity<>(response, headers(request), response.getStatus());
    }

    @Hidden
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(TableMalformedException.class)
    public ResponseEntity<ApiErrorDto> handle(TableMalformedException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.BAD_REQUEST)
                .message(e.getLocalizedMessage())
                .code("error.table.invalid")
                .build();
        return new ResponseEntity<>(response, headers(request), response.getStatus());
    }

    @Hidden
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(TableNotFoundException.class)
    public ResponseEntity<ApiErrorDto> handle(TableNotFoundException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.NOT_FOUND)
                .message(e.getLocalizedMessage())
                .code("error.table.missing")
                .build();
        return new ResponseEntity<>(response, headers(request), response.getStatus());
    }

    @Hidden
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiErrorDto> handle(UserNotFoundException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.NOT_FOUND)
                .message(e.getLocalizedMessage())
                .code("error.user.missing")
                .build();
        return new ResponseEntity<>(response, headers(request), response.getStatus());
    }

}
