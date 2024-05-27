package at.tuwien.gateway;

import at.tuwien.ExportResourceDto;
import at.tuwien.api.database.AccessTypeDto;
import at.tuwien.api.database.DatabaseDto;
import at.tuwien.api.database.ViewCreateDto;
import at.tuwien.api.database.ViewDto;
import at.tuwien.api.database.internal.CreateDatabaseDto;
import at.tuwien.api.database.query.QueryDto;
import at.tuwien.api.database.table.TableCreateDto;
import at.tuwien.api.database.table.TableDto;
import at.tuwien.api.user.internal.UpdateUserPasswordDto;
import at.tuwien.exception.*;

import java.util.List;
import java.util.UUID;

public interface DataServiceGateway {
    void createAccess(Long databaseId, UUID userId, AccessTypeDto access) throws ServiceConnectionException, ServiceException, DatabaseNotFoundException;

    void updateAccess(Long databaseId, UUID userId, AccessTypeDto access) throws ServiceConnectionException, ServiceException, AccessNotFoundException;

    void deleteAccess(Long databaseId, UUID userId) throws ServiceConnectionException, ServiceException, AccessNotFoundException;

    DatabaseDto createDatabase(CreateDatabaseDto data) throws ServiceConnectionException, ServiceException;

    void updateDatabase(Long databaseId, UpdateUserPasswordDto data) throws ServiceConnectionException, ServiceException, DatabaseNotFoundException;

    void createTable(Long databaseId, TableCreateDto data) throws ServiceConnectionException, ServiceException, DatabaseNotFoundException, TableExistsException;

    void deleteTable(Long databaseId, Long tableId) throws ServiceConnectionException, ServiceException, TableNotFoundException;

    ViewDto createView(Long databaseId, ViewCreateDto data) throws ServiceConnectionException, ServiceException;

    void deleteView(Long databaseId, Long viewId) throws ServiceConnectionException, ServiceException, ViewNotFoundException;

    QueryDto findQuery(Long databaseId, Long queryId) throws ServiceConnectionException, ServiceException, QueryNotFoundException;

    ExportResourceDto exportQuery(Long databaseId, Long queryId) throws ServiceConnectionException, ServiceException, QueryNotFoundException;

    List<TableDto> getTableSchemas(Long databaseId) throws ServiceConnectionException, ServiceException, QueryNotFoundException;

    List<ViewDto> getViewSchemas(Long databaseId) throws ServiceConnectionException, ServiceException, QueryNotFoundException;
}
