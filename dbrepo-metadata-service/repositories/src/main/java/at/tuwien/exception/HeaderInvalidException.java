package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.BAD_REQUEST)
public class HeaderInvalidException extends Exception {

    public HeaderInvalidException(String msg) {
        super(msg);
    }

    public HeaderInvalidException(String msg, Throwable thr) {
        super(msg + ": " + thr.getLocalizedMessage(), thr);
    }

    public HeaderInvalidException(Throwable thr) {
        super(thr);
    }

}
