package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.BAD_REQUEST)
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
