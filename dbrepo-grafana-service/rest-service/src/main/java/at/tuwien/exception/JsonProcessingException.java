package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR)
public class JsonProcessingException extends RuntimeException {

    public JsonProcessingException(String message) {
        super(message);
    }

    public JsonProcessingException(String message, Throwable thr) {
        super(message, thr);
    }

}
