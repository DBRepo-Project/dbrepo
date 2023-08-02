package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.FORBIDDEN)
public class AccessDeniedException extends Exception {

    public AccessDeniedException(String msg) {
        super(msg);
    }

    public AccessDeniedException(String msg, Throwable thr) {
        super(msg, thr);
    }

    public AccessDeniedException(Throwable thr) {
        super(thr);
    }

}
