package at.ac.tuwien.ifs.dbrepo.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "error.database.missing")
public class DatabaseNotFoundException extends Exception {

    public DatabaseNotFoundException(String msg) {
        super(msg);
    }

    public DatabaseNotFoundException(String msg, Throwable thr) {
        super(msg + ": " + thr.getLocalizedMessage(), thr);
    }

    public DatabaseNotFoundException(Throwable thr) {
        super(thr);
    }

}
