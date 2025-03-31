package at.ac.tuwien.ifs.dbrepo.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "error.message.missing")
public class MessageNotFoundException extends Exception {

    public MessageNotFoundException(String msg) {
        super(msg);
    }

    public MessageNotFoundException(String msg, Throwable thr) {
        super(msg + ": " + thr.getLocalizedMessage(), thr);
    }

    public MessageNotFoundException(Throwable thr) {
        super(thr);
    }

}
