package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR)
public class RestTemplateExchangeException extends RuntimeException {

    public RestTemplateExchangeException(String message) {
        super(message);
    }

    public RestTemplateExchangeException(String message, Throwable thr) {
        super(message, thr);
    }

}
