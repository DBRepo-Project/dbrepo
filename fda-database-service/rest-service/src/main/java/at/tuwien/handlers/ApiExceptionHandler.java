package at.tuwien.handlers;

import at.tuwien.api.error.ApiErrorDto;
import at.tuwien.exception.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorDto> handle(AccessDeniedException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.NOT_FOUND)
                .message(e.getLocalizedMessage())
                .code("error.database.access")
                .build();
        return new ResponseEntity<>(response, new HttpHeaders(), response.getStatus());
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(AmqpException.class)
    public ResponseEntity<ApiErrorDto> handle(AmqpException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.NOT_FOUND)
                .message(e.getLocalizedMessage())
                .code("error.amqp.exchange")
                .build();
        return new ResponseEntity<>(response, new HttpHeaders(), response.getStatus());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(BrokerMalformedException.class)
    public ResponseEntity<ApiErrorDto> handle(BrokerMalformedException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.BAD_REQUEST)
                .message(e.getLocalizedMessage())
                .code("error.amqp.exchange")
                .build();
        return new ResponseEntity<>(response, new HttpHeaders(), response.getStatus());
    }

    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    @ExceptionHandler(ContainerConnectionException.class)
    public ResponseEntity<ApiErrorDto> handle(ContainerConnectionException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.BAD_GATEWAY)
                .message(e.getLocalizedMessage())
                .code("error.container.connection")
                .build();
        return new ResponseEntity<>(response, new HttpHeaders(), response.getStatus());
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(ContainerNotFoundException.class)
    public ResponseEntity<ApiErrorDto> handle(ContainerNotFoundException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.NOT_FOUND)
                .message(e.getLocalizedMessage())
                .code("error.container.notfound")
                .build();
        return new ResponseEntity<>(response, new HttpHeaders(), response.getStatus());
    }

    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    @ExceptionHandler(DatabaseConnectionException.class)
    public ResponseEntity<ApiErrorDto> handle(DatabaseConnectionException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .message(e.getLocalizedMessage())
                .code("error.database.connection")
                .build();
        return new ResponseEntity<>(response, new HttpHeaders(), response.getStatus());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(DatabaseMalformedException.class)
    public ResponseEntity<ApiErrorDto> handle(DatabaseMalformedException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.BAD_REQUEST)
                .message(e.getLocalizedMessage())
                .code("error.database.malformed")
                .build();
        return new ResponseEntity<>(response, new HttpHeaders(), response.getStatus());
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(DatabaseNameExistsException.class)
    public ResponseEntity<ApiErrorDto> handle(DatabaseNameExistsException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.CONFLICT)
                .message(e.getLocalizedMessage())
                .code("error.database.name_exists")
                .build();
        return new ResponseEntity<>(response, new HttpHeaders(), response.getStatus());
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(DatabaseNotFoundException.class)
    public ResponseEntity<ApiErrorDto> handle(DatabaseNotFoundException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.NOT_FOUND)
                .message(e.getLocalizedMessage())
                .code("error.database.notfound")
                .build();
        return new ResponseEntity<>(response, new HttpHeaders(), response.getStatus());
    }

    @ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
    @ExceptionHandler(ImageNotSupportedException.class)
    public ResponseEntity<ApiErrorDto> handle(ImageNotSupportedException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.NOT_ACCEPTABLE)
                .message(e.getLocalizedMessage())
                .code("error.image.notsupported")
                .build();
        return new ResponseEntity<>(response, new HttpHeaders(), response.getStatus());
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(LicenseNotFoundException.class)
    public ResponseEntity<ApiErrorDto> handle(LicenseNotFoundException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.NOT_FOUND)
                .message(e.getLocalizedMessage())
                .code("error.license.notfound")
                .build();
        return new ResponseEntity<>(response, new HttpHeaders(), response.getStatus());
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(QueryStoreException.class)
    public ResponseEntity<ApiErrorDto> handle(QueryStoreException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.CONFLICT)
                .message(e.getLocalizedMessage())
                .code("error.querystore.remote")
                .build();
        return new ResponseEntity<>(response, new HttpHeaders(), response.getStatus());
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(SubjectNotFoundException.class)
    public ResponseEntity<ApiErrorDto> handle(SubjectNotFoundException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.NOT_FOUND)
                .message(e.getLocalizedMessage())
                .code("error.subject.notfound")
                .build();
        return new ResponseEntity<>(response, new HttpHeaders(), response.getStatus());
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiErrorDto> handle(UserNotFoundException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.NOT_FOUND)
                .message(e.getLocalizedMessage())
                .code("error.user.notfound")
                .build();
        return new ResponseEntity<>(response, new HttpHeaders(), response.getStatus());
    }

}