package at.ac.tuwien.ifs.dbrepo.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.EXPECTATION_FAILED, reason = "error.store.persist")
public class QueryStorePersistException extends Exception {

    public QueryStorePersistException(String message) {
        super(message);
    }

    public QueryStorePersistException(String message, Throwable thr) {
        super(message, thr);
    }

    public QueryStorePersistException(Throwable thr) {
        super(thr);
    }

}
