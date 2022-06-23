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

    @ResponseStatus(HttpStatus.EXPECTATION_FAILED)
    @ExceptionHandler(DraftRecordCreateException.class)
    public ResponseEntity<ApiErrorDto> handle(DraftRecordCreateException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.EXPECTATION_FAILED)
                .message(e.getLocalizedMessage())
                .code("error.document.create")
                .build();
        return new ResponseEntity<>(response, new HttpHeaders(), response.getStatus());
    }

}