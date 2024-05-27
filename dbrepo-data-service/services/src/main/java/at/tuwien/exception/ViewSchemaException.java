package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.CONFLICT, reason = "error.schema.view")
public class ViewSchemaException extends Exception {

    public ViewSchemaException(String message) {
        super(message);
    }

    public ViewSchemaException(String message, Throwable thr) {
        super(message, thr);
    }

    public ViewSchemaException(Throwable thr) {
        super(thr);
    }

}
