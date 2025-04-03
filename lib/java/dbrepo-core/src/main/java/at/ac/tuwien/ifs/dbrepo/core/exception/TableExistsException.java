package at.ac.tuwien.ifs.dbrepo.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.CONFLICT, reason = "error.table.exists")
public class TableExistsException extends Exception {

    public TableExistsException(String msg) {
        super(msg);
    }

    public TableExistsException(String msg, Throwable thr) {
        super(msg + ": " + thr.getLocalizedMessage(), thr);
    }

    public TableExistsException(Throwable thr) {
        super(thr);
    }

}
