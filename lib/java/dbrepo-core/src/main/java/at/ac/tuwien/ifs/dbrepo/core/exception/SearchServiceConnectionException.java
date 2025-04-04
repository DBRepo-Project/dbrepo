package at.ac.tuwien.ifs.dbrepo.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.BAD_GATEWAY, reason = "error.search.connection")
public class SearchServiceConnectionException extends Exception {

    public SearchServiceConnectionException(String msg) {
        super(msg);
    }

    public SearchServiceConnectionException(String msg, Throwable thr) {
        super(msg + ": " + thr.getLocalizedMessage(), thr);
    }

    public SearchServiceConnectionException(Throwable thr) {
        super(thr);
    }

}
