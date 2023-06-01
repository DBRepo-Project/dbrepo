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
    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(ContainerAlreadyExistsException.class)
    public ResponseEntity<ApiErrorDto> handle(ContainerAlreadyExistsException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.CONFLICT)
                .message(e.getLocalizedMessage())
                .code("error.container.exists")
                .build();
        return new ResponseEntity<>(response, new HttpHeaders(), response.getStatus());
    }

    @Hidden
    @ResponseStatus(HttpStatus.GONE)
    @ExceptionHandler(ContainerAlreadyRemovedException.class)
    public ResponseEntity<ApiErrorDto> handle(ContainerAlreadyRemovedException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.GONE)
                .message(e.getLocalizedMessage())
                .code("error.container.alreadyremoved")
                .build();
        return new ResponseEntity<>(response, new HttpHeaders(), response.getStatus());
    }

    @Hidden
    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(ContainerAlreadyRunningException.class)
    public ResponseEntity<ApiErrorDto> handle(ContainerAlreadyRunningException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.CONFLICT)
                .message(e.getLocalizedMessage())
                .code("error.container.alreadyrunning")
                .build();
        return new ResponseEntity<>(response, new HttpHeaders(), response.getStatus());
    }

    @Hidden
    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(ContainerAlreadyStoppedException.class)
    public ResponseEntity<ApiErrorDto> handle(ContainerAlreadyStoppedException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.CONFLICT)
                .message(e.getLocalizedMessage())
                .code("error.container.alreadystopped")
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
                .code("error.container.notfound")
                .build();
        return new ResponseEntity<>(response, new HttpHeaders(), response.getStatus());
    }

    @Hidden
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    @ExceptionHandler(ContainerNotRunningException.class)
    public ResponseEntity<ApiErrorDto> handle(ContainerNotRunningException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.BAD_GATEWAY)
                .message(e.getLocalizedMessage())
                .code("error.container.notrunning")
                .build();
        return new ResponseEntity<>(response, new HttpHeaders(), response.getStatus());
    }

    @Hidden
    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(ContainerStillRunningException.class)
    public ResponseEntity<ApiErrorDto> handle(ContainerStillRunningException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.CONFLICT)
                .message(e.getLocalizedMessage())
                .code("error.container.stillrunning")
                .build();
        return new ResponseEntity<>(response, new HttpHeaders(), response.getStatus());
    }

    @Hidden
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    @ExceptionHandler(DockerClientException.class)
    public ResponseEntity<ApiErrorDto> handle(DockerClientException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.BAD_GATEWAY)
                .message(e.getLocalizedMessage())
                .code("error.container.dockerclient")
                .build();
        return new ResponseEntity<>(response, new HttpHeaders(), response.getStatus());
    }

    @Hidden
    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(ImageAlreadyExistsException.class)
    public ResponseEntity<ApiErrorDto> handle(ImageAlreadyExistsException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.CONFLICT)
                .message(e.getLocalizedMessage())
                .code("error.image.exists")
                .build();
        return new ResponseEntity<>(response, new HttpHeaders(), response.getStatus());
    }

    @Hidden
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ImageInvalidException.class)
    public ResponseEntity<ApiErrorDto> handle(ImageInvalidException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.BAD_REQUEST)
                .message(e.getLocalizedMessage())
                .code("error.image.invalid")
                .build();
        return new ResponseEntity<>(response, new HttpHeaders(), response.getStatus());
    }

    @Hidden
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(ImageNotFoundException.class)
    public ResponseEntity<ApiErrorDto> handle(ImageNotFoundException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.NOT_FOUND)
                .message(e.getLocalizedMessage())
                .code("error.image.notfound")
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
                .code("error.container.notallowed")
                .build();
        return new ResponseEntity<>(response, new HttpHeaders(), response.getStatus());
    }

    @Hidden
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(PersistenceException.class)
    public ResponseEntity<ApiErrorDto> handle(PersistenceException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.FORBIDDEN)
                .message(e.getLocalizedMessage())
                .code("error.container.storage")
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
                .code("error.container.usernotfound")
                .build();
        return new ResponseEntity<>(response, new HttpHeaders(), response.getStatus());
    }

}