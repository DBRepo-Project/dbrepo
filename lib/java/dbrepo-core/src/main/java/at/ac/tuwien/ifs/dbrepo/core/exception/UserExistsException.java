package at.ac.tuwien.ifs.dbrepo.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.CONFLICT, reason = "error.user.exists")
public class UserExistsException extends Exception {

    public UserExistsException(String message) {
        super(message);
    }

    public UserExistsException(String message, Throwable thr) {
        super(message, thr);
    }

    public UserExistsException(Throwable thr) {
        super(thr);
    }

}
