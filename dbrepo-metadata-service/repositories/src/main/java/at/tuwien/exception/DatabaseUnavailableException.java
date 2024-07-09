package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.SERVICE_UNAVAILABLE, reason = "error.database.connection")
public class DatabaseUnavailableException extends Exception {

    public DatabaseUnavailableException(String message) {
        super(message);
    }

    public DatabaseUnavailableException(String message, Throwable thr) {
        super(message, thr);
    }

    public DatabaseUnavailableException(Throwable thr) {
        super(thr);
    }

}
