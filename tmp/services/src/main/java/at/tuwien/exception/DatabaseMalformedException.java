package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.BAD_REQUEST)
public class DatabaseMalformedException extends Exception {

    public DatabaseMalformedException(String message) {
        super(message);
    }

    public DatabaseMalformedException(String message, Throwable thr) {
        super(message, thr);
    }

    public DatabaseMalformedException(Throwable thr) {
        super(thr);
    }

}
