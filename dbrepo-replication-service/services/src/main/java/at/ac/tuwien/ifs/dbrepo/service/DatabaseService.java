package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.ViewDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.internal.TableCreateDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.DatabaseNotificationDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.internal.UpdateUserPasswordDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;

import java.sql.SQLException;
import java.util.List;

public interface DatabaseService {


    /**
     * Handles database replication notification.
     * @param databaseNotificationDto The database notification containing replication information.
     */
    void handleDatabaseReplication(DatabaseNotificationDto databaseNotificationDto);
}
