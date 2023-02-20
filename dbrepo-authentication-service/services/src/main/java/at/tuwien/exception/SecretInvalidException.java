package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.EXPECTATION_FAILED)
public class SecretInvalidException extends Exception {

    public SecretInvalidException(String msg) {
        super(msg);
    }

    public SecretInvalidException(String msg, Throwable thr) {
        super(msg, thr);
    }

    public SecretInvalidException(Throwable thr) {
        super(thr);
    }
}
