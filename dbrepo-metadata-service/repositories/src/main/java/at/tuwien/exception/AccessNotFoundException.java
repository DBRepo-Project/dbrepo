package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "error.access.missing")
public class AccessNotFoundException extends Exception {

    public AccessNotFoundException(String msg) {
        super(msg);
    }

    public AccessNotFoundException(String msg, Throwable thr) {
        super(msg + ": " + thr.getLocalizedMessage(), thr);
    }

    public AccessNotFoundException(Throwable thr) {
        super(thr);
    }

}
