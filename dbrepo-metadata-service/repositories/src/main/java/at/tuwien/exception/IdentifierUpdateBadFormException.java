package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.BAD_REQUEST)
public class IdentifierUpdateBadFormException extends Exception {

    public IdentifierUpdateBadFormException(String msg) {
        super(msg);
    }

    public IdentifierUpdateBadFormException(String msg, Throwable thr) {
        super(msg + ": " + thr.getLocalizedMessage(), thr);
    }

    public IdentifierUpdateBadFormException(Throwable thr) {
        super(thr);
    }

}
