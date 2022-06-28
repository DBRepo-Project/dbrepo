package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.FAILED_DEPENDENCY)
public class FileUploadException extends Exception {

    public FileUploadException(String msg) {
        super(msg);
    }

    public FileUploadException(String msg, Throwable thr) {
        super(msg, thr);
    }

    public FileUploadException(Throwable thr) {
        super(thr);
    }

}
