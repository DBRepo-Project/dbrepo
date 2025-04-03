package at.ac.tuwien.ifs.dbrepo.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.SERVICE_UNAVAILABLE, reason = "error.dashboard.invalid")
public class DashboardServiceException extends Exception {

    public DashboardServiceException(String message) {
        super(message);
    }

    public DashboardServiceException(String message, Throwable thr) {
        super(message, thr);
    }

    public DashboardServiceException(Throwable thr) {
        super(thr);
    }

}
