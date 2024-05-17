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

    /**
     * Creates a view in the given data database.
     *
     * @param database The data database.
     * @param data     The view.
     * @throws SQLException           The connection to the data database was unsuccessful.
     * @throws ViewMalformedException The query is malformed and was rejected by the data database.
     */
    void create(PrivilegedDatabaseDto database, ViewCreateDto data) throws SQLException,
            ViewMalformedException;

    /**
     * Get data from the given view at specific timestamp, paginated by page and size.
     *
     * @param view      The view.
     * @param timestamp The timestamp.
     * @param page      The page number.
     * @param size      The page size.
     * @return The data, if successful.
     * @throws SQLException           The connection to the data database was unsuccessful.
     * @throws ViewMalformedException The query is malformed and was rejected by the data database.
     */
    QueryResultDto data(PrivilegedViewDto view, Instant timestamp, Long page, Long size) throws SQLException,
            ViewMalformedException;

    void delete(PrivilegedViewDto view) throws SQLException, ViewMalformedException;

    Long count(PrivilegedViewDto view, Instant timestamp) throws SQLException, QueryMalformedException;

    ExportResourceDto exportDataset(PrivilegedDatabaseDto database, ViewDto view, Instant timestamp)
            throws SQLException, QueryMalformedException, SidecarExportException, StorageNotFoundException,
            StorageUnavailableException;
}
