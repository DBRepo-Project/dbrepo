package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.IOException;

@ResponseStatus(code = HttpStatus.CONFLICT)
public class DatabaseNameExistsException extends IOException {

    public DatabaseNameExistsException(String msg) {
        super(msg);
    }

    public DatabaseNameExistsException(String msg, Throwable thr) {
        super(msg + ": " + thr.getLocalizedMessage(), thr);
    }

    public DatabaseNameExistsException(Throwable thr) {
        super(thr);
    }

}
