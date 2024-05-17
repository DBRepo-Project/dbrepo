package at.tuwien.service;

import at.tuwien.ExportResourceDto;
import at.tuwien.api.database.ViewCreateDto;
import at.tuwien.api.database.ViewDto;
import at.tuwien.api.database.internal.PrivilegedDatabaseDto;
import at.tuwien.api.database.internal.PrivilegedViewDto;
import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.exception.*;

import java.sql.SQLException;
import java.time.Instant;

public interface ViewService {
    void create(PrivilegedDatabaseDto database, ViewCreateDto data) throws SQLException,
            DatabaseMalformedException;

    QueryResultDto data(PrivilegedDatabaseDto database, ViewDto view, Instant timestamp, Long page,
                        Long size) throws SQLException, TableMalformedException;

    void delete(PrivilegedViewDto view) throws SQLException,
            DatabaseMalformedException;

    Long count(PrivilegedDatabaseDto database, ViewDto view, Instant timestamp) throws SQLException,
            QueryMalformedException;

    ExportResourceDto exportDataset(PrivilegedDatabaseDto database, ViewDto view, Instant timestamp)
            throws SQLException, QueryMalformedException, SidecarExportException, StorageNotFoundException,
            StorageUnavailableException;
}
