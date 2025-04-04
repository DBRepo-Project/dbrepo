package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_FOUND)
public class TableNotFoundException extends Exception {

    public TableNotFoundException(String message) {
        super(message);
    }

    public TableNotFoundException(String message, Throwable thr) {
        super(message, thr);
    }

    public TableNotFoundException(Throwable thr) {
        super(thr);
    }

}
