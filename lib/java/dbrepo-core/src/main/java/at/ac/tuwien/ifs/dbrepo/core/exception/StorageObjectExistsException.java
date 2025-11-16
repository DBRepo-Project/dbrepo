package at.ac.tuwien.ifs.dbrepo.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.CONFLICT, reason = "error.storage.exists")
public class StorageObjectExistsException extends Exception {

    public StorageObjectExistsException(String message) {
        super(message);
    }

    public StorageObjectExistsException(String message, Throwable thr) {
        super(message, thr);
    }

    public StorageObjectExistsException(Throwable thr) {
        super(thr);
    }

}
