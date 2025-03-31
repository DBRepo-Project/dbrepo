package at.ac.tuwien.ifs.dbrepo.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "error.storage.missing")
public class StorageNotFoundException extends Exception {

    public StorageNotFoundException(String message) {
        super(message);
    }

    public StorageNotFoundException(String message, Throwable thr) {
        super(message, thr);
    }

    public StorageNotFoundException(Throwable thr) {
        super(thr);
    }

}
