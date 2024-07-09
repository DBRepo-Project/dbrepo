package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "error.store.clean")
public class QueryStoreGCException extends Exception {

    public QueryStoreGCException(String message) {
        super(message);
    }

    public QueryStoreGCException(String message, Throwable thr) {
        super(message, thr);
    }

    public QueryStoreGCException(Throwable thr) {
        super(thr);
    }

}
