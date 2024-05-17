package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "error.license.missing")
public class LicenseNotFoundException extends Exception {

    public LicenseNotFoundException(String msg) {
        super(msg);
    }

    public LicenseNotFoundException(String msg, Throwable thr) {
        super(msg + ": " + thr.getLocalizedMessage(), thr);
    }

    public LicenseNotFoundException(Throwable thr) {
        super(thr);
    }

}
