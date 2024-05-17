package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "error.request.invalid")
public class MalformedException extends Exception {

    public MalformedException(String msg) {
        super(msg);
    }

    public MalformedException(String msg, Throwable thr) {
        super(msg + ": " + thr.getLocalizedMessage(), thr);
    }

    public MalformedException(Throwable thr) {
        super(thr);
    }

}
