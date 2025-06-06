package at.ac.tuwien.ifs.dbrepo.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "error.column.missing")
public class ColumnNotFoundException extends Exception {

    public ColumnNotFoundException(String msg) {
        super(msg);
    }

    public ColumnNotFoundException(String msg, Throwable thr) {
        super(msg + ": " + thr.getLocalizedMessage(), thr);
    }

    public ColumnNotFoundException(Throwable thr) {
        super(thr);
    }

}
