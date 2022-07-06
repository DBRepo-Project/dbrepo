package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.EXPECTATION_FAILED)
public class UserInvenioTokenException extends Exception {

    public UserInvenioTokenException(String msg) {
        super(msg);
    }

    public UserInvenioTokenException(String msg, Throwable thr) {
        super(msg, thr);
    }

    public UserInvenioTokenException(Throwable thr) {
        super(thr);
    }

}
