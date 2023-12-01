package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_FOUND)
public class TableColumnNotFoundException extends Exception {

    public TableColumnNotFoundException(String msg) {
        super(msg);
    }

    public TableColumnNotFoundException(String msg, Throwable thr) {
        super(msg + ": " + thr.getLocalizedMessage(), thr);
    }

    public TableColumnNotFoundException(Throwable thr) {
        super(thr);
    }

}
