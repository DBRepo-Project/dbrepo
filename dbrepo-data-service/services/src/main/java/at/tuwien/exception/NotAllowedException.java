package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.FORBIDDEN, reason = "error.request.forbidden")
public class NotAllowedException extends Exception {

    public NotAllowedException(String message) {
        super(message);
    }

    public NotAllowedException(String message, Throwable thr) {
        super(message, thr);
    }

    public NotAllowedException(Throwable thr) {
        super(thr);
    }

}
