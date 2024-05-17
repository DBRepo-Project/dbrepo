package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "error.doi.missing")
public class DoiNotFoundException extends Exception {

    public DoiNotFoundException(String msg) {
        super(msg);
    }

    public DoiNotFoundException(String msg, Throwable thr) {
        super(msg + ": " + thr.getLocalizedMessage(), thr);
    }

    public DoiNotFoundException(Throwable thr) {
        super(thr);
    }

}
