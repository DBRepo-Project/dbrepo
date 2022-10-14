package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "View not found")
public class ViewNotFoundException extends Exception {

    public ViewNotFoundException(String message) {
        super(message);
    }

    public ViewNotFoundException(String message, Throwable thr) {
        super(message, thr);
    }

    public ViewNotFoundException(Throwable thr) {
        super(thr);
    }

}
