package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.IOException;

@ResponseStatus(code = HttpStatus.NO_CONTENT)
public class DatabaseUnchangedException extends IOException {

    public DatabaseUnchangedException(String msg) {
        super(msg);
    }

    public DatabaseUnchangedException(String msg, Throwable thr) {
        super(msg + ": " + thr.getLocalizedMessage(), thr);
    }

    public DatabaseUnchangedException(Throwable thr) {
        super(thr);
    }

}
