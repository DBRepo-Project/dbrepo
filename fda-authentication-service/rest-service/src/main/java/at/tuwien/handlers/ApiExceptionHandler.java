package at.tuwien.handlers;

import at.tuwien.api.error.ApiErrorDto;
import at.tuwien.exception.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(AuthenticationInvalidException.class)
    public ResponseEntity<ApiErrorDto> handle(AuthenticationInvalidException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.FORBIDDEN)
                .message(e.getLocalizedMessage())
                .code("error.auth.invalid")
                .build();
        return new ResponseEntity<>(response, new HttpHeaders(), response.getStatus());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(AuthenticationMalformedException.class)
    public ResponseEntity<ApiErrorDto> handle(AuthenticationMalformedException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.BAD_REQUEST)
                .message(e.getLocalizedMessage())
                .code("error.auth.malformed")
                .build();
        return new ResponseEntity<>(response, new HttpHeaders(), response.getStatus());
    }

    @ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
    @ExceptionHandler(BrokerUserCreationException.class)
    public ResponseEntity<ApiErrorDto> handle(BrokerUserCreationException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.NOT_ACCEPTABLE)
                .message(e.getLocalizedMessage())
                .code("error.auth.createbrokeruser")
                .build();
        return new ResponseEntity<>(response, new HttpHeaders(), response.getStatus());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(OrcidMalformedException.class)
    public ResponseEntity<ApiErrorDto> handle(OrcidMalformedException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.BAD_REQUEST)
                .message(e.getLocalizedMessage())
                .code("error.auth.orcid")
                .build();
        return new ResponseEntity<>(response, new HttpHeaders(), response.getStatus());
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(RoleNotFoundException.class)
    public ResponseEntity<ApiErrorDto> handle(RoleNotFoundException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.NOT_FOUND)
                .message(e.getLocalizedMessage())
                .code("error.auth.role")
                .build();
        return new ResponseEntity<>(response, new HttpHeaders(), response.getStatus());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(RoleUniqueException.class)
    public ResponseEntity<ApiErrorDto> handle(RoleUniqueException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.BAD_REQUEST)
                .message(e.getLocalizedMessage())
                .code("error.auth.unique-role")
                .build();
        return new ResponseEntity<>(response, new HttpHeaders(), response.getStatus());
    }

    @ResponseStatus(HttpStatus.EXPECTATION_FAILED)
    @ExceptionHandler(SecretInvalidException.class)
    public ResponseEntity<ApiErrorDto> handle(SecretInvalidException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.EXPECTATION_FAILED)
                .message(e.getLocalizedMessage())
                .code("error.auth.secretinvalid")
                .build();
        return new ResponseEntity<>(response, new HttpHeaders(), response.getStatus());
    }

    @ResponseStatus(HttpStatus.EXPECTATION_FAILED)
    @ExceptionHandler(TokenNotEligableException.class)
    public ResponseEntity<ApiErrorDto> handle(TokenNotEligableException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.EXPECTATION_FAILED)
                .message(e.getLocalizedMessage())
                .code("error.auth.tokennoteligable")
                .build();
        return new ResponseEntity<>(response, new HttpHeaders(), response.getStatus());
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(TokenNotFoundException.class)
    public ResponseEntity<ApiErrorDto> handle(TokenNotFoundException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.NOT_FOUND)
                .message(e.getLocalizedMessage())
                .code("error.auth.tokennotfound")
                .build();
        return new ResponseEntity<>(response, new HttpHeaders(), response.getStatus());
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(TokenRevokedException.class)
    public ResponseEntity<ApiErrorDto> handle(TokenRevokedException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.UNAUTHORIZED)
                .message(e.getLocalizedMessage())
                .code("error.auth.tokenrevoked")
                .build();
        return new ResponseEntity<>(response, new HttpHeaders(), response.getStatus());
    }

    @ResponseStatus(HttpStatus.FAILED_DEPENDENCY)
    @ExceptionHandler(UserEmailAlreadyVerifiedException.class)
    public ResponseEntity<ApiErrorDto> handle(UserEmailAlreadyVerifiedException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.FAILED_DEPENDENCY)
                .message(e.getLocalizedMessage())
                .code("error.auth.emailisverified")
                .build();
        return new ResponseEntity<>(response, new HttpHeaders(), response.getStatus());
    }

    @ResponseStatus(HttpStatus.EXPECTATION_FAILED)
    @ExceptionHandler(UserEmailExistsException.class)
    public ResponseEntity<ApiErrorDto> handle(UserEmailExistsException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.EXPECTATION_FAILED)
                .message(e.getLocalizedMessage())
                .code("error.auth.email")
                .build();
        return new ResponseEntity<>(response, new HttpHeaders(), response.getStatus());
    }

    @ResponseStatus(HttpStatus.PRECONDITION_REQUIRED)
    @ExceptionHandler(UserEmailFailedException.class)
    public ResponseEntity<ApiErrorDto> handle(UserEmailFailedException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.PRECONDITION_REQUIRED)
                .message(e.getLocalizedMessage())
                .code("error.auth.emaildelivery")
                .build();
        return new ResponseEntity<>(response, new HttpHeaders(), response.getStatus());
    }

    @ResponseStatus(HttpStatus.I_AM_A_TEAPOT)
    @ExceptionHandler(UserEmailNotVerifiedException.class)
    public ResponseEntity<ApiErrorDto> handle(UserEmailNotVerifiedException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.I_AM_A_TEAPOT)
                .message(e.getLocalizedMessage())
                .code("error.auth.emailnotverified")
                .build();
        return new ResponseEntity<>(response, new HttpHeaders(), response.getStatus());
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(UserNameExistsException.class)
    public ResponseEntity<ApiErrorDto> handle(UserNameExistsException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.CONFLICT)
                .message(e.getLocalizedMessage())
                .code("error.auth.username")
                .build();
        return new ResponseEntity<>(response, new HttpHeaders(), response.getStatus());
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiErrorDto> handle(UserNotFoundException e, WebRequest request) {
        final ApiErrorDto response = ApiErrorDto.builder()
                .status(HttpStatus.NOT_FOUND)
                .message(e.getLocalizedMessage())
                .code("error.auth.user")
                .build();
        return new ResponseEntity<>(response, new HttpHeaders(), response.getStatus());
    }

}