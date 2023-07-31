package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "License not found")
public class LicenseNotFoundException extends Exception {

    public LicenseNotFoundException(String message) {
        super(message);
    }

    public LicenseNotFoundException(String message, Throwable thr) {
        super(message, thr);
    }

    public LicenseNotFoundException(Throwable thr) {
        super(thr);
    }

}
