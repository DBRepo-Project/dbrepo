package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.CONFLICT, reason = "error.schema.table")
public class TableSchemaException extends Exception {

    public TableSchemaException(String message) {
        super(message);
    }

    public TableSchemaException(String message, Throwable thr) {
        super(message, thr);
    }

    public TableSchemaException(Throwable thr) {
        super(thr);
    }

}
