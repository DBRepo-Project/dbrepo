package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.BAD_REQUEST)
public class OrcidMalformedException extends Exception {

    public OrcidMalformedException(String msg) {
        super(msg);
    }

    public OrcidMalformedException(String msg, Throwable thr) {
        super(msg, thr);
    }

    public OrcidMalformedException(Throwable thr) {
        super(thr);
    }
}
