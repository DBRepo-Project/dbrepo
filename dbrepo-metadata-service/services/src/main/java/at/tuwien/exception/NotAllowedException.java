package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.METHOD_NOT_ALLOWED)
public class NotAllowedException extends Exception {

    public NotAllowedException(String msg) {
        super(msg);
    }

    public NotAllowedException(String msg, Throwable thr) {
        super(msg, thr);
    }

    public NotAllowedException(Throwable thr) {
        super(thr);
    }

}
