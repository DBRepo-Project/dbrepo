package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "error.view.invalid")
public class ViewMalformedException extends Exception {

    public ViewMalformedException(String message) {
        super(message);
    }

    public ViewMalformedException(String message, Throwable thr) {
        super(message, thr);
    }

    public ViewMalformedException(Throwable thr) {
        super(thr);
    }

}
