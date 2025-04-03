package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_FOUND)
public class QueryNotFoundException extends Exception {

    public QueryNotFoundException(String message) {
        super(message);
    }

    public QueryNotFoundException(String message, Throwable thr) {
        super(message, thr);
    }

    public QueryNotFoundException(Throwable thr) {
        super(thr);
    }

}
