package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.CONFLICT, reason = "error.table.exists")
public class TableExistsException extends Exception {

    public TableExistsException(String message) {
        super(message);
    }

    public TableExistsException(String message, Throwable thr) {
        super(message, thr);
    }

    public TableExistsException(Throwable thr) {
        super(thr);
    }

}
