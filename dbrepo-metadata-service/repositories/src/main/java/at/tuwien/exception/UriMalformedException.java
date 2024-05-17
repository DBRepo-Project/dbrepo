package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.EXPECTATION_FAILED, reason = "error.semantics.uri")
public class UriMalformedException extends Exception {

    public UriMalformedException(String msg) {
        super(msg);
    }

    public UriMalformedException(String msg, Throwable thr) {
        super(msg + ": " + thr.getLocalizedMessage(), thr);
    }

    public UriMalformedException(Throwable thr) {
        super(thr);
    }

}
