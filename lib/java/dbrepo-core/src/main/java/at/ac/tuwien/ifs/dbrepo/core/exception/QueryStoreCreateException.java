package at.ac.tuwien.ifs.dbrepo.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.EXPECTATION_FAILED, reason = "error.store.invalid")
public class QueryStoreCreateException extends Exception {

    public QueryStoreCreateException(String message) {
        super(message);
    }

    public QueryStoreCreateException(String message, Throwable thr) {
        super(message, thr);
    }

    public QueryStoreCreateException(Throwable thr) {
        super(thr);
    }

}
