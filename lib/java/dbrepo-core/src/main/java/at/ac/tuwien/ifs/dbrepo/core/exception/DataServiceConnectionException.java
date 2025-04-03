package at.ac.tuwien.ifs.dbrepo.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.BAD_GATEWAY, reason = "error.data.connection")
public class DataServiceConnectionException extends Exception {

    public DataServiceConnectionException(String msg) {
        super(msg);
    }

    public DataServiceConnectionException(String msg, Throwable thr) {
        super(msg + ": " + thr.getLocalizedMessage(), thr);
    }

    public DataServiceConnectionException(Throwable thr) {
        super(thr);
    }

}
