package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "error.exchange.missing")
public class ExchangeNotFoundException extends Exception {

    public ExchangeNotFoundException(String msg) {
        super(msg);
    }

    public ExchangeNotFoundException(String msg, Throwable thr) {
        super(msg + ": " + thr.getLocalizedMessage(), thr);
    }

    public ExchangeNotFoundException(Throwable thr) {
        super(thr);
    }

}
