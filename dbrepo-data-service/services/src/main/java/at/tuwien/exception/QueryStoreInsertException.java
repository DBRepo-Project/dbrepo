package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "error.store.insert")
public class QueryStoreInsertException extends Exception {

    public QueryStoreInsertException(String message) {
        super(message);
    }

    public QueryStoreInsertException(String message, Throwable thr) {
        super(message, thr);
    }

    public QueryStoreInsertException(Throwable thr) {
        super(thr);
    }

}
