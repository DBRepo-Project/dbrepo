package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.SERVICE_UNAVAILABLE, reason = "error.storage.missing")
public class StorageUnavailableException extends Exception {

    public StorageUnavailableException(String message) {
        super(message);
    }

    public StorageUnavailableException(String message, Throwable thr) {
        super(message, thr);
    }

    public StorageUnavailableException(Throwable thr) {
        super(thr);
    }

}
