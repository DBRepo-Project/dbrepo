package at.ac.tuwien.ifs.dbrepo.exception;

import at.ac.tuwien.ifs.dbrepo.core.exception.DashboardServiceException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Checked: retain the database permission change when a later dashboard call fails. */
@ResponseStatus(code = HttpStatus.BAD_GATEWAY, reason = "error.access.dashboard_sync")
public class DashboardAccessSyncException extends DashboardServiceException {
    public DashboardAccessSyncException(Throwable cause) {
        super("Database access was saved, but dashboard permissions could not be synchronized. "
                + "If the recipient has not used the dashboard, they must sign in there once. Retry the synchronization.", cause);
    }
}
