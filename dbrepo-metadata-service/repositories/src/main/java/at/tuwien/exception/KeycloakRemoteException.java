package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.LOCKED)
public class KeycloakRemoteException extends Exception {

    public KeycloakRemoteException(String msg) {
        super(msg);
    }

    public KeycloakRemoteException(String msg, Throwable thr) {
        super(msg, thr);
    }

    public KeycloakRemoteException(Throwable thr) {
        super(thr);
    }

}
