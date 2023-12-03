package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.UNPROCESSABLE_ENTITY)
public class SemanticEntityPersistException extends Exception {

    public SemanticEntityPersistException(String msg) {
        super(msg);
    }

    public SemanticEntityPersistException(String msg, Throwable thr) {
        super(msg + ": " + thr.getLocalizedMessage(), thr);
    }

    public SemanticEntityPersistException(Throwable thr) {
        super(thr);
    }

}
