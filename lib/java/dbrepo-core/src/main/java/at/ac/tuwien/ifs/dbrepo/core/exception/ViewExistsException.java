package at.ac.tuwien.ifs.dbrepo.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.CONFLICT, reason = "error.view.exists")
public class ViewExistsException extends Exception {

    public ViewExistsException(String msg) {
        super(msg);
    }

    public ViewExistsException(String msg, Throwable thr) {
        super(msg + ": " + thr.getLocalizedMessage(), thr);
    }

    public ViewExistsException(Throwable thr) {
        super(thr);
    }

}
