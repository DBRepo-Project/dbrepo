package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_FOUND)
public class IdentifierNotFoundException extends Exception {

    public IdentifierNotFoundException(String message) {
        super(message);
    }

    public IdentifierNotFoundException(String message, Throwable thr) {
        super(message, thr);
    }

    public IdentifierNotFoundException(Throwable thr) {
        super(thr);
    }

}
