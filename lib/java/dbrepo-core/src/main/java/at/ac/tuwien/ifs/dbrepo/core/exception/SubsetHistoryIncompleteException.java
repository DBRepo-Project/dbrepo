package at.ac.tuwien.ifs.dbrepo.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.CONFLICT, reason = "error.subset.history.incomplete")
public class SubsetHistoryIncompleteException extends RuntimeException {
    public SubsetHistoryIncompleteException(String message) { super(message); }
}
