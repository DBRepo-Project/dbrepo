package at.tuwien.handlers;

import at.tuwien.api.error.ApiErrorDto;
import at.tuwien.exception.*;
import io.swagger.v3.oas.annotations.Hidden;
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

    @Hidden
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorDto> handle(AccessDeniedException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.FORBIDDEN)
                .message(e.getLocalizedMessage())
                .code("error.database.access")
                .build();
        return new ResponseEntity<>(response, new HttpHeaders(), response.getStatus());
    }

    @Hidden
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(AmqpException.class)
    public ResponseEntity<ApiErrorDto> handle(AmqpException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.NOT_FOUND)
                .message(e.getLocalizedMessage())
                .code("error.database.amqp")
                .build();
        return new ResponseEntity<>(response, new HttpHeaders(), response.getStatus());
    }

    @Hidden
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(BrokerMalformedException.class)
    public ResponseEntity<ApiErrorDto> handle(BrokerMalformedException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.BAD_REQUEST)
                .message(e.getLocalizedMessage())
                .code("error.database.broker")
                .build();
        return new ResponseEntity<>(response, new HttpHeaders(), response.getStatus());
    }

    @Hidden
    @ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
    @ExceptionHandler(BrokerVirtualHostCreationException.class)
    public ResponseEntity<ApiErrorDto> handle(BrokerVirtualHostCreationException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.NOT_ACCEPTABLE)
                .message(e.getLocalizedMessage())
                .code("error.database.virtualhostcreate")
                .build();
        return new ResponseEntity<>(response, new HttpHeaders(), response.getStatus());
    }

    @Hidden
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    @ExceptionHandler(BrokerVirtualHostGrantException.class)
    public ResponseEntity<ApiErrorDto> handle(BrokerVirtualHostGrantException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .message(e.getLocalizedMessage())
                .code("error.database.virtualhostgrant")
                .build();
        return new ResponseEntity<>(response, new HttpHeaders(), response.getStatus());
    }

    @Hidden
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    @ExceptionHandler(ContainerConnectionException.class)
    public ResponseEntity<ApiErrorDto> handle(ContainerConnectionException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .message(e.getLocalizedMessage())
                .code("error.database.containerconnection")
                .build();
        return new ResponseEntity<>(response, new HttpHeaders(), response.getStatus());
    }

    @Hidden
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(ContainerNotFoundException.class)
    public ResponseEntity<ApiErrorDto> handle(ContainerNotFoundException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.NOT_FOUND)
                .message(e.getLocalizedMessage())
                .code("error.database.containernotfound")
                .build();
        return new ResponseEntity<>(response, new HttpHeaders(), response.getStatus());
    }

    @Hidden
    @ResponseStatus(HttpStatus.EXPECTATION_FAILED)
    @ExceptionHandler(ContainerUnauthorizedException.class)
    public ResponseEntity<ApiErrorDto> handle(ContainerUnauthorizedException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.EXPECTATION_FAILED)
                .message(e.getLocalizedMessage())
                .code("error.database.containerunauthorized")
                .build();
        return new ResponseEntity<>(response, new HttpHeaders(), response.getStatus());
    }

    @Hidden
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    @ExceptionHandler(DatabaseConnectionException.class)
    public ResponseEntity<ApiErrorDto> handle(DatabaseConnectionException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .message(e.getLocalizedMessage())
                .code("error.database.databaseconnection")
                .build();
        return new ResponseEntity<>(response, new HttpHeaders(), response.getStatus());
    }

    @Hidden
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(DatabaseMalformedException.class)
    public ResponseEntity<ApiErrorDto> handle(DatabaseMalformedException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.BAD_REQUEST)
                .message(e.getLocalizedMessage())
                .code("error.database.databasemalformed")
                .build();
        return new ResponseEntity<>(response, new HttpHeaders(), response.getStatus());
    }

    @Hidden
    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(DatabaseNameExistsException.class)
    public ResponseEntity<ApiErrorDto> handle(DatabaseNameExistsException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.CONFLICT)
                .message(e.getLocalizedMessage())
                .code("error.database.databasenameexists")
                .build();
        return new ResponseEntity<>(response, new HttpHeaders(), response.getStatus());
    }

    @Hidden
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(DatabaseNotFoundException.class)
    public ResponseEntity<ApiErrorDto> handle(DatabaseNotFoundException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.NOT_FOUND)
                .message(e.getLocalizedMessage())
                .code("error.database.databasenotfound")
                .build();
        return new ResponseEntity<>(response, new HttpHeaders(), response.getStatus());
    }

    @Hidden
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(IdentifierNotFoundException.class)
    public ResponseEntity<ApiErrorDto> handle(IdentifierNotFoundException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.NOT_FOUND)
                .message(e.getLocalizedMessage())
                .code("error.database.identifiernotfound")
                .build();
        return new ResponseEntity<>(response, new HttpHeaders(), response.getStatus());
    }

    @Hidden
    @ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
    @ExceptionHandler(ImageNotSupportedException.class)
    public ResponseEntity<ApiErrorDto> handle(ImageNotSupportedException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.NOT_IMPLEMENTED)
                .message(e.getLocalizedMessage())
                .code("error.database.imagenotsupported")
                .build();
        return new ResponseEntity<>(response, new HttpHeaders(), response.getStatus());
    }

    @Hidden
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(LicenseNotFoundException.class)
    public ResponseEntity<ApiErrorDto> handle(LicenseNotFoundException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.NOT_FOUND)
                .message(e.getLocalizedMessage())
                .code("error.database.licensenotfound")
                .build();
        return new ResponseEntity<>(response, new HttpHeaders(), response.getStatus());
    }

    @Hidden
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    @ExceptionHandler(NotAllowedException.class)
    public ResponseEntity<ApiErrorDto> handle(NotAllowedException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .message(e.getLocalizedMessage())
                .code("error.database.notallowed")
                .build();
        return new ResponseEntity<>(response, new HttpHeaders(), response.getStatus());
    }

    @Hidden
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(QueryMalformedException.class)
    public ResponseEntity<ApiErrorDto> handle(QueryMalformedException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.BAD_REQUEST)
                .message(e.getLocalizedMessage())
                .code("error.database.querymalformed")
                .build();
        return new ResponseEntity<>(response, new HttpHeaders(), response.getStatus());
    }

    @Hidden
    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(QueryStoreException.class)
    public ResponseEntity<ApiErrorDto> handle(QueryStoreException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.CONFLICT)
                .message(e.getLocalizedMessage())
                .code("error.database.querystore")
                .build();
        return new ResponseEntity<>(response, new HttpHeaders(), response.getStatus());
    }

    @Hidden
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(SubjectNotFoundException.class)
    public ResponseEntity<ApiErrorDto> handle(SubjectNotFoundException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.NOT_FOUND)
                .message(e.getLocalizedMessage())
                .code("error.database.subjectnotfound")
                .build();
        return new ResponseEntity<>(response, new HttpHeaders(), response.getStatus());
    }

    @Hidden
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiErrorDto> handle(UserNotFoundException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.NOT_FOUND)
                .message(e.getLocalizedMessage())
                .code("error.database.usernotfound")
                .build();
        return new ResponseEntity<>(response, new HttpHeaders(), response.getStatus());
    }

}