package at.ac.tuwien.ifs.dbrepo.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "error.identifier.unsupported")
public class IdentifierNotSupportedException extends Exception {

    public IdentifierNotSupportedException(String msg) {
        super(msg);
    }

    public IdentifierNotSupportedException(String msg, Throwable thr) {
        super(msg + ": " + thr.getLocalizedMessage(), thr);
    }

    public IdentifierNotSupportedException(Throwable thr) {
        super(thr);
    }

}
