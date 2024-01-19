package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.CONFLICT)
public class QueryAlreadyPersistedException extends Exception {

    public QueryAlreadyPersistedException(String msg) {
        super(msg);
    }

    public QueryAlreadyPersistedException(String msg, Throwable thr) {
        super(msg + ": " + thr.getLocalizedMessage(), thr);
    }

    public QueryAlreadyPersistedException(Throwable thr) { super(thr);
    }
}
