package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_FOUND)
public class UserAttributeNotFoundException extends Exception {

    public UserAttributeNotFoundException(String msg) {
        super(msg);
    }

    public UserAttributeNotFoundException(String msg, Throwable thr) {
        super(msg + ": " + thr.getLocalizedMessage(), thr);
    }

    public UserAttributeNotFoundException(Throwable thr) {
        super(thr);
    }

}
