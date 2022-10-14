package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.EXPECTATION_FAILED)
public class TokenNotEligableException extends Exception {

    public TokenNotEligableException(String msg) {
        super(msg);
    }

    public TokenNotEligableException(String msg, Throwable thr) {
        super(msg, thr);
    }

    public TokenNotEligableException(Throwable thr) {
        super(thr);
    }
}
