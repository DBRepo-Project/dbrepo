package at.tuwien.service;

import at.tuwien.ExportResourceDto;
import at.tuwien.api.database.internal.PrivilegedDatabaseDto;
import at.tuwien.api.database.query.ImportCsvDto;
import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.api.database.table.*;
import at.tuwien.api.database.table.internal.PrivilegedTableDto;
import at.tuwien.api.database.table.internal.TableCreateDto;
import at.tuwien.exception.*;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

public interface TableService {
    void createTable(PrivilegedDatabaseDto database, TableCreateDto data) throws SQLException,
            TableMalformedException, TableExistsException;

    void delete(PrivilegedTableDto table) throws SQLException, QueryMalformedException;

    QueryResultDto getData(PrivilegedTableDto table, Instant timestamp, Long page,
                        Long size) throws SQLException, TableMalformedException;

    List<TableHistoryDto> history(PrivilegedTableDto table) throws SQLException,
            TableNotFoundException;

    Long getCount(PrivilegedTableDto table, Instant timestamp) throws SQLException,
            QueryMalformedException;

    void importTuple(PrivilegedTableDto table, TupleDto data)
            throws TableMalformedException, StorageUnavailableException, StorageNotFoundException, SQLException, QueryMalformedException;

    void importDataset(PrivilegedTableDto table, ImportCsvDto data)
            throws SidecarImportException, StorageNotFoundException, SQLException, QueryMalformedException;

    void deleteTuple(PrivilegedTableDto table, TupleDeleteDto data) throws SQLException,
            TableMalformedException, QueryMalformedException;

    void createTuple(PrivilegedTableDto table, TupleDto data) throws SQLException,
            QueryMalformedException, TableMalformedException;

    void updateTuple(PrivilegedTableDto table, TupleUpdateDto data) throws SQLException,
            QueryMalformedException, TableMalformedException;

    ExportResourceDto exportDataset(PrivilegedTableDto table, Instant timestamp)
            throws SQLException, SidecarExportException, StorageNotFoundException, StorageUnavailableException,
            QueryMalformedException;
}
