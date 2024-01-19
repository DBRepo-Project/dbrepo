package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.SERVICE_UNAVAILABLE)
public class BrokerRemoteException extends Exception {

    public BrokerRemoteException(String msg) {
        super(msg);
    }

    public BrokerRemoteException(String msg, Throwable thr) {
        super(msg + ": " + thr.getLocalizedMessage(), thr);
    }

    public BrokerRemoteException(Throwable thr) {
        super(thr);
    }

}
