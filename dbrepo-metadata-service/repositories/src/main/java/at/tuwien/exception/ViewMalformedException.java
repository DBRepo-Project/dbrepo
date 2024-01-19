package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.LOCKED)
public class ViewMalformedException extends Exception {

    public ViewMalformedException(String msg) {
        super(msg);
    }

    public ViewMalformedException(String msg, Throwable thr) {
        super(msg + ": " + thr.getLocalizedMessage(), thr);
    }

    public ViewMalformedException(Throwable thr) {
        super(thr);
    }

}
