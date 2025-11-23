package at.ac.tuwien.ifs.dbrepo.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.UNPROCESSABLE_ENTITY, reason = "error.query.execution")
public class QueryExecutionException extends Exception {

    public QueryExecutionException(String message) {
        super(message);
    }

    public QueryExecutionException(String message, Throwable thr) {
        super(message, thr);
    }

    public QueryExecutionException(Throwable thr) {
        super(thr);
    }

}
