package at.tuwien.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.CONFLICT)
public class CommitFileUploadException extends Exception {

    public CommitFileUploadException(String msg) {
        super(msg);
    }

    public CommitFileUploadException(String msg, Throwable thr) {
        super(msg, thr);
    }

    public CommitFileUploadException(Throwable thr) {
        super(thr);
    }

}
