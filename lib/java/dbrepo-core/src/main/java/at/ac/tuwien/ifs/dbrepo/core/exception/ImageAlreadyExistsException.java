package at.ac.tuwien.ifs.dbrepo.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.CONFLICT, reason = "error.image.exists")
public class ImageAlreadyExistsException extends Exception {

    public ImageAlreadyExistsException(String msg) {
        super(msg);
    }

    public ImageAlreadyExistsException(String msg, Throwable thr) {
        super(msg + ": " + thr.getLocalizedMessage(), thr);
    }

    public ImageAlreadyExistsException(Throwable thr) {
        super(thr);
    }

}