package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.CONFLICT)
public class TupleDeleteException extends Exception {

    public TupleDeleteException(String msg) {
        super(msg);
    }

    public TupleDeleteException(String msg, Throwable thr) {
        super(msg + ": " + thr.getLocalizedMessage(), thr);
    }

    public TupleDeleteException(Throwable thr) { super(thr);
    }
}
