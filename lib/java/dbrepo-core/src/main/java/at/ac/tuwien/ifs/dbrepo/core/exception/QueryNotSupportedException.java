package at.ac.tuwien.ifs.dbrepo.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_IMPLEMENTED, reason = "error.query.invalid")
public class QueryNotSupportedException extends Exception {

    public QueryNotSupportedException(String message) {
        super(message);
    }

    public QueryNotSupportedException(String message, Throwable thr) {
        super(message, thr);
    }

    public QueryNotSupportedException(Throwable thr) {
        super(thr);
    }

}
