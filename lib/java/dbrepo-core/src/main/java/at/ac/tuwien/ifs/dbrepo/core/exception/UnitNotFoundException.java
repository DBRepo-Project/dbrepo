package at.ac.tuwien.ifs.dbrepo.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "error.unit.missing")
public class UnitNotFoundException extends Exception {

    public UnitNotFoundException(String msg) {
        super(msg);
    }

    public UnitNotFoundException(String msg, Throwable thr) {
        super(msg + ": " + thr.getLocalizedMessage(), thr);
    }

    public UnitNotFoundException(Throwable thr) {
        super(thr);
    }

}
