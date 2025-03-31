package at.ac.tuwien.ifs.dbrepo.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.BAD_GATEWAY, reason = "error.dashboard.connection")
public class DashboardServiceConnectionException extends Exception {

    public DashboardServiceConnectionException(String msg) {
        super(msg);
    }

    public DashboardServiceConnectionException(String msg, Throwable thr) {
        super(msg + ": " + thr.getLocalizedMessage(), thr);
    }

    public DashboardServiceConnectionException(Throwable thr) {
        super(thr);
    }

}
