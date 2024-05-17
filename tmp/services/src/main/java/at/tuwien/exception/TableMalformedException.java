package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.BAD_REQUEST)
public class TableMalformedException extends Exception {

    public TableMalformedException(String message) {
        super(message);
    }

    public TableMalformedException(String message, Throwable thr) {
        super(message, thr);
    }

    public TableMalformedException(Throwable thr) {
        super(thr);
    }

}
