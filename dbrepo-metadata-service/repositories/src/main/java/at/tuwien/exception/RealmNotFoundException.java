package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_FOUND)
public class RealmNotFoundException extends Exception {

    public RealmNotFoundException(String msg) {
        super(msg);
    }

    public RealmNotFoundException(String msg, Throwable thr) {
        super(msg + ": " + thr.getLocalizedMessage(), thr);
    }

    public RealmNotFoundException(Throwable thr) {
        super(thr);
    }

}
