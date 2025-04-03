package at.ac.tuwien.ifs.dbrepo.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "error.query.invalid")
public class QueryMalformedException extends Exception {

    public QueryMalformedException(String message) {
        super(message);
    }

    public QueryMalformedException(String message, Throwable thr) {
        super(message, thr);
    }

    public QueryMalformedException(Throwable thr) {
        super(thr);
    }

}
