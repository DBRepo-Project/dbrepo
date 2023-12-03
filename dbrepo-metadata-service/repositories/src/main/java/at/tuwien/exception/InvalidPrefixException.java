package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.BAD_REQUEST)
public class InvalidPrefixException extends Exception {

    public InvalidPrefixException(String msg) {
        super(msg);
    }

    public InvalidPrefixException(String msg, Throwable thr) {
        super(msg + ": " + thr.getLocalizedMessage(), thr);
    }

    public InvalidPrefixException(Throwable thr) {
        super(thr);
    }
}
