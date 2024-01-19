package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.BAD_REQUEST)
public class IdentifierRequestException extends Exception {

    public IdentifierRequestException(String msg) {
        super(msg);
    }

    public IdentifierRequestException(String msg, Throwable thr) {
        super(msg + ": " + thr.getLocalizedMessage(), thr);
    }

    public IdentifierRequestException(Throwable thr) {
        super(thr);
    }

}
