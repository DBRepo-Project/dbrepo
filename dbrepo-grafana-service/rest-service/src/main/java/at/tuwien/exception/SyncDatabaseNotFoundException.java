package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR)
public class SyncDatabaseNotFoundException extends RuntimeException {

    public SyncDatabaseNotFoundException(String message) {
        super(message);
    }

    public SyncDatabaseNotFoundException(String message, Throwable thr) {
        super(message, thr);
    }

}
