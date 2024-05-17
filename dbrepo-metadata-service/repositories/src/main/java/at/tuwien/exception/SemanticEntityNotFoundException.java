package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "error.semantic.missing")
public class SemanticEntityNotFoundException extends Exception {

    public SemanticEntityNotFoundException(String msg) {
        super(msg);
    }

    public SemanticEntityNotFoundException(String msg, Throwable thr) {
        super(msg + ": " + thr.getLocalizedMessage(), thr);
    }

    public SemanticEntityNotFoundException(Throwable thr) {
        super(thr);
    }

}
