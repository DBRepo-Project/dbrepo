package at.ac.tuwien.ifs.dbrepo.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.CONFLICT, reason = "error.subset.integrity.mismatch")
public class SubsetIntegrityMismatchException extends RuntimeException {
    public SubsetIntegrityMismatchException(String message) { super(message); }
}
