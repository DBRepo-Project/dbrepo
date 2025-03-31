package at.ac.tuwien.ifs.dbrepo.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.FORBIDDEN, reason = "error.request.forbidden")
public class NotAllowedException extends Exception {

    public NotAllowedException(String msg) {
        super(msg);
    }

    public NotAllowedException(String msg, Throwable thr) {
        super(msg + ": " + thr.getLocalizedMessage(), thr);
    }

    public NotAllowedException(Throwable thr) {
        super(thr);
    }

}
