package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.EXPECTATION_FAILED)
public class ColumnParseException extends Exception {

    public ColumnParseException(String msg) {
        super(msg);
    }

    public ColumnParseException(String msg, Throwable thr) {
        super(msg + ": " + thr.getLocalizedMessage(), thr);
    }

    public ColumnParseException(Throwable thr) {
        super(thr);
    }

}
