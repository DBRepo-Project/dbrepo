package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.METHOD_NOT_ALLOWED)
public class ForeignUserException extends Exception {

    public ForeignUserException(String message) {
        super(message);
    }

    public ForeignUserException(String message, Throwable thr) {
        super(message, thr);
    }

    public ForeignUserException(Throwable thr) {
        super(thr);
    }

}
