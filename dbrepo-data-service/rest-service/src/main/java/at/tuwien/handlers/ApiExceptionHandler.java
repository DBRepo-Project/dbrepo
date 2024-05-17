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

    @Hidden
    @ResponseStatus(code = HttpStatus.NOT_FOUND)
    @ExceptionHandler(ContainerNotFoundException.class)
    public ResponseEntity<ApiErrorDto> handle(ContainerNotFoundException e) {
        return generic_handle(e.getClass(), e.getLocalizedMessage());
    }

    @Hidden
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
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
    @ResponseStatus(code = HttpStatus.NOT_ACCEPTABLE)
    @ExceptionHandler(FormatNotAvailableException.class)
    public ResponseEntity<ApiErrorDto> handle(FormatNotAvailableException e) {
        return generic_handle(e.getClass(), e.getLocalizedMessage());
    }

    @Hidden
    @ResponseStatus(code = HttpStatus.FORBIDDEN)
    @ExceptionHandler(NotAllowedException.class)
    public ResponseEntity<ApiErrorDto> handle(NotAllowedException e) {
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
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
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
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    @ExceptionHandler(QueryStoreInsertException.class)
    public ResponseEntity<ApiErrorDto> handle(QueryStoreInsertException e) {
        return generic_handle(e.getClass(), e.getLocalizedMessage());
    }

    @Hidden
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
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
    @ResponseStatus(code = HttpStatus.BAD_GATEWAY)
    @ExceptionHandler(ServiceConnectionException.class)
    public ResponseEntity<ApiErrorDto> handle(ServiceConnectionException e) {
        return generic_handle(e.getClass(), e.getLocalizedMessage());
    }

    @Hidden
    @ResponseStatus(code = HttpStatus.SERVICE_UNAVAILABLE)
    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<ApiErrorDto> handle(ServiceException e) {
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
    @ResponseStatus(code = HttpStatus.NOT_FOUND)
    @ExceptionHandler(TableNotFoundException.class)
    public ResponseEntity<ApiErrorDto> handle(TableNotFoundException e) {
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

    private ResponseEntity<ApiErrorDto> generic_handle(Class<?> exceptionClass, String message) {
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
