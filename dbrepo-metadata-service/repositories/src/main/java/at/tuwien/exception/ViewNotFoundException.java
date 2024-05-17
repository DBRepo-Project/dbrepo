package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "error.view.missing")
public class ViewNotFoundException extends Exception {

    public ViewNotFoundException(String msg) {
        super(msg);
    }

    public ViewNotFoundException(String msg, Throwable thr) {
        super(msg + ": " + thr.getLocalizedMessage(), thr);
    }

    public ViewNotFoundException(Throwable thr) {
        super(thr);
    }

}
