package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "error.ror.missing")
public class RorNotFoundException extends Exception {

    public RorNotFoundException(String msg) {
        super(msg);
    }

    public RorNotFoundException(String msg, Throwable thr) {
        super(msg + ": " + thr.getLocalizedMessage(), thr);
    }

    public RorNotFoundException(Throwable thr) {
        super(thr);
    }

}
