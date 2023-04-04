package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.BAD_REQUEST)
public class RoleUniqueException extends Exception {

    public RoleUniqueException(String msg) {
        super(msg);
    }

    public RoleUniqueException(String msg, Throwable thr) {
        super(msg, thr);
    }

    public RoleUniqueException(Throwable thr) {
        super(thr);
    }
}
