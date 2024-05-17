package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "error.request.sort")
public class SortException extends Exception {

    public SortException(String msg) {
        super(msg);
    }

    public SortException(String msg, Throwable thr) {
        super(msg + ": " + thr.getLocalizedMessage(), thr);
    }

    public SortException(Throwable thr) {
        super(thr);
    }

}
