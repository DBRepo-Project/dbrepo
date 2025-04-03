package at.ac.tuwien.ifs.dbrepo.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.PRECONDITION_REQUIRED, reason = "error.user.setup")
public class AccountNotSetupException extends Exception {

    public AccountNotSetupException(String msg) {
        super(msg);
    }

    public AccountNotSetupException(String msg, Throwable thr) {
        super(msg + ": " + thr.getLocalizedMessage(), thr);
    }

    public AccountNotSetupException(Throwable thr) {
        super(thr);
    }

}
