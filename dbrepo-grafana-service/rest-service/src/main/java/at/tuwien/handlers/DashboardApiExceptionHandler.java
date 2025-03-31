package at.tuwien.handlers;

import at.tuwien.api.error.ApiErrorDto;
import at.tuwien.exception.RestTemplateExchangeException;
import at.tuwien.exception.JsonProcessingException;
import at.tuwien.exception.SyncDatabaseNotFoundException;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@Log4j2
@ControllerAdvice
public class DashboardApiExceptionHandler {

    @Hidden
    @ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(JsonProcessingException.class)
    public ResponseEntity<ApiErrorDto> handle(JsonProcessingException e) {
        return genericHandle(e.getClass(), e.getLocalizedMessage());
    }

    @Hidden
    @ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(RestTemplateExchangeException.class)
    public ResponseEntity<ApiErrorDto> handle(RestTemplateExchangeException e) {
        return genericHandle(e.getClass(), e.getLocalizedMessage());
    }

    @Hidden
    @ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(SyncDatabaseNotFoundException.class)
    public ResponseEntity<ApiErrorDto> handle(SyncDatabaseNotFoundException e) {
        return genericHandle(e.getClass(), e.getLocalizedMessage());
    }

    private ResponseEntity<ApiErrorDto> genericHandle(Class<?> exceptionClass, String message) {
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
