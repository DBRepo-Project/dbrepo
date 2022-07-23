package at.tuwien.service.impl;

import at.tuwien.ExportResource;
import at.tuwien.InsertTableRawQuery;
import at.tuwien.api.database.query.ExecuteStatementDto;
import at.tuwien.api.database.query.ImportDto;
import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.api.database.table.TableCsvDeleteDto;
import at.tuwien.api.database.table.TableCsvDto;
import at.tuwien.api.database.table.TableCsvUpdateDto;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.database.table.columns.TableColumn;
import at.tuwien.exception.*;
import at.tuwien.mapper.QueryMapper;
import at.tuwien.querystore.Query;
import at.tuwien.service.*;
import lombok.extern.log4j.Log4j2;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserManager;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.NativeQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.PersistenceException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.math.BigInteger;
import java.security.Principal;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Log4j2
@Service
public class QueryServiceImpl extends HibernateConnector implements QueryService {

    private final QueryMapper queryMapper;
    private final TableService tableService;
    private final StoreService storeService;
    private final DatabaseService databaseService;

    @Autowired
    public QueryServiceImpl(QueryMapper queryMapper, TableService tableService, DatabaseService databaseService,
                            StoreService storeService) {
        this.queryMapper = queryMapper;
        this.tableService = tableService;
        this.databaseService = databaseService;
        this.storeService = storeService;
    }

    @Override
    @Transactional(readOnly = true)
    public QueryResultDto execute(Long containerId, Long databaseId, ExecuteStatementDto statement,
                                  Principal principal, Long page, Long size)
            throws DatabaseNotFoundException, ImageNotSupportedException, QueryMalformedException, QueryStoreException,
            ContainerNotFoundException, ColumnParseException, UserNotFoundException, TableMalformedException {
        Query q = storeService.insert(containerId, databaseId, null, statement, principal, Instant.now());
        final QueryResultDto result = this.reExecute(containerId, databaseId, q, page, size);
        q = storeService.update(containerId, databaseId, result, result.getResultNumber(), q);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public QueryResultDto reExecute(Long containerId, Long databaseId, Query query, Long page, Long size)
            throws QueryMalformedException, DatabaseNotFoundException, ImageNotSupportedException,
            ColumnParseException, TableMalformedException {
        /* find */
        final Database database = databaseService.find(containerId, databaseId);
        if (!database.getContainer().getImage().getRepository().equals("mariadb")) {
            throw new ImageNotSupportedException("Currently only MariaDB is supported");
        }
        /* run query */
        final Session session = getCurrentSession(database.getContainer().getImage(), database.getContainer(), database);
        final Transaction transaction = session.beginTransaction();
        /* prepare the statement */
        final NativeQuery<?> nativeQuery = session.createSQLQuery(
                queryMapper.queryToRawTimestampedQuery(query.getQuery(), database, query.getExecution(), page, size));
        final List<?> result;
        try {
            log.debug("affected {} rows", nativeQuery.executeUpdate());
            result = nativeQuery.getResultList();
            activeConnection(session);
            transaction.commit();
        } catch (PersistenceException e) {
            log.error("Query not valid for this database");
            session.close();
            throw new QueryMalformedException("Query not valid for this database", e);
        } finally {
            if (session.isOpen()) {
                session.close();
            }
        }
        /* map the result to the tables (with respective columns) from the statement metadata */
        final List<TableColumn> columns;
        try {
            columns = parseColumns(query, database);
        } catch (JSQLParserException e) {
            log.error("Failed to map/parse columns.");
            throw new ColumnParseException("Failed to map/parse columns", e);
        }
        final QueryResultDto dto = queryMapper.resultListToQueryResultDto(columns, result);
        dto.setId(query.getId());
        dto.setResultNumber(countQueryResults(containerId, databaseId, query));
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public QueryResultDto findAll(Long containerId, Long databaseId, Long tableId, Instant timestamp, Long page,
                                  Long size) throws TableNotFoundException, DatabaseNotFoundException,
            ImageNotSupportedException, DatabaseConnectionException, TableMalformedException, PaginationException,
            ContainerNotFoundException {
        /* find */
        final Database database = databaseService.find(containerId, databaseId);
        final Table table = tableService.find(containerId, databaseId, tableId);
        /* run query */
        final Session session = getCurrentSession(database.getContainer().getImage(), database.getContainer(), database);
        final Transaction transaction = session.beginTransaction();
        final NativeQuery<?> query = session.createSQLQuery(
                queryMapper.tableToRawFindAllQuery(table, timestamp, size, page));
        final List<?> resultList;
        try {
            log.debug("affected {} tuples in database id {}", query.executeUpdate(), databaseId);
            resultList = query.getResultList();
            activeConnection(session);
            transaction.commit();
        } catch (PersistenceException e) {
            log.error("Failed to find data");
            session.close();
            throw new TableMalformedException("\"Failed to find data", e);
        } finally {
            if (session.isOpen()) {
                session.close();
            }
        }
        final QueryResultDto result;
        try {
            result = queryMapper.queryTableToQueryResultDto(resultList, table);
        } catch (DateTimeException e) {
            log.error("Failed to parse date from the one stored in the metadata database");
            throw new TableMalformedException("Could not parse date from format", e);
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public ExportResource findAll(Long containerId, Long databaseId, Long tableId, Instant timestamp)
            throws TableNotFoundException, DatabaseNotFoundException, ImageNotSupportedException,
            DatabaseConnectionException, TableMalformedException, PaginationException, ContainerNotFoundException,
            FileStorageException {
        final String filename = RandomStringUtils.randomAlphabetic(40) + ".csv";
        /* find */
        final Database database = databaseService.find(containerId, databaseId);
        final Table table = tableService.find(containerId, databaseId, tableId);
        /* run query */
        final Session session = getCurrentSession(database.getContainer().getImage(), database.getContainer(), database);
        final Transaction transaction = session.beginTransaction();
        final NativeQuery<?> query = session.createSQLQuery(
                queryMapper.tableToRawExportQuery(table, timestamp, filename));
        try {
            log.debug("affected tuples {}", query.executeUpdate());
            activeConnection(session);
            transaction.commit();
        } catch (PersistenceException e) {
            log.error("Failed to export table");
            session.close();
            throw new TableMalformedException("Data not found", e);
        } finally {
            if (session.isOpen()) {
                session.close();
            }
        }
        /* read file */
        final InputStream inputStream;
        try {
            inputStream = FileUtils.openInputStream(new File("/tmp/" + filename));
        } catch (IOException e) {
            throw new FileStorageException("Export file not present");
        }
        return ExportResource.builder()
                .resource(new InputStreamResource(inputStream))
                .filename(filename)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ExportResource findOne(Long containerId, Long databaseId, Long queryId)
            throws DatabaseNotFoundException, ImageNotSupportedException, TableMalformedException,
            ContainerNotFoundException, FileStorageException, QueryStoreException, QueryNotFoundException,
            QueryMalformedException {
        final String filename = RandomStringUtils.randomAlphabetic(40) + ".csv";
        /* find */
        final Database database = databaseService.find(containerId, databaseId);
        final Query query = storeService.findOne(containerId, databaseId, queryId);
        /* run query */
        final Session session = getCurrentSession(database.getContainer().getImage(), database.getContainer(), database);
        final Transaction transaction = session.beginTransaction();
        final NativeQuery<?> query2 = session.createSQLQuery(queryMapper.queryToRawExportQuery(query, filename));
        try {
            log.debug("affected tuples {}", query2.executeUpdate());
            activeConnection(session);
            transaction.commit();
        } catch (PersistenceException e) {
            log.error("Failed to export query");
            session.close();
            throw new TableMalformedException("Failed to export query", e);
        } finally {
            if (session.isOpen()) {
                session.close();
            }
        }
        /* read file */
        final InputStream inputStream;
        try {
            inputStream = FileUtils.openInputStream(new File("/tmp/" + filename));
        } catch (IOException e) {
            log.error("Export file not present");
            throw new FileStorageException("Export file not present", e);
        }
        final InputStreamResource resource = new InputStreamResource(inputStream);
        return ExportResource.builder()
                .resource(resource)
                .filename(filename)
                .build();
    }

    @Override
    @Transactional
    public BigInteger count(Long containerId, Long databaseId, Long tableId, Instant timestamp)
            throws DatabaseNotFoundException, TableNotFoundException,
            TableMalformedException, ImageNotSupportedException {
        /* find */
        final Database database = databaseService.find(containerId, databaseId);
        final Table table = tableService.find(containerId, databaseId, tableId);
        /* run query */
        final Session session = getCurrentSession(database.getContainer().getImage(), database.getContainer(), database);
        final Transaction transaction = session.beginTransaction();
        final NativeQuery<BigInteger> query = session.createSQLQuery(
                queryMapper.tableToRawCountAllQuery(table, timestamp));
        final BigInteger count;
        try {
            log.info("counted {} tuples in table id {}", query.executeUpdate(), tableId);
            count = query.getSingleResult();
            activeConnection(session);
            transaction.commit();
        } catch (PersistenceException e) {
            log.error("Failed to count tuples");
            session.close();
            throw new TableMalformedException("Failed to count tuples", e);
        } finally {
            if (session.isOpen()) {
                session.close();
            }
        }
        return count;
    }

    @Override
    @Transactional
    public Integer insert(Long containerId, Long databaseId, Long tableId, TableCsvDto data)
            throws ImageNotSupportedException, TableMalformedException, DatabaseNotFoundException,
            TableNotFoundException, ContainerNotFoundException {
        /* find */
        final Database database = databaseService.find(containerId, databaseId);
        final Table table = tableService.find(containerId, databaseId, tableId);
        /* run query */
        if (data.getData().size() == 0) return null;
        final Session session = getCurrentSession(database.getContainer().getImage(), database.getContainer(), database);
        final Transaction transaction = session.beginTransaction();
        activeConnection(session);
        /* prepare the statement */
        final InsertTableRawQuery raw;
        try {
            raw = queryMapper.tableCsvDtoToRawInsertQuery(table, data);
        } catch (DateTimeParseException e) {
            log.error("Failed to parse date: {}", e.getMessage());
            session.close();
            return 0;
        } catch (NumberFormatException e) {
            log.error("Failed to parse number: {}", e.getMessage());
            session.close();
            return 0;
        } catch (Exception e) {
            log.error("Failed for unknown reason: {}", e.getMessage());
            session.close();
            return 0;
        }
        final NativeQuery<?> query = session.createSQLQuery(raw.getQuery());
        log.trace("query with parameters {}", query.setParameterList(1, raw.getData()));
        return execute(query, session);
    }

    @Override
    @Transactional
    public Integer update(Long containerId, Long databaseId, Long tableId, TableCsvUpdateDto data)
            throws ImageNotSupportedException, TableMalformedException, DatabaseNotFoundException,
            TableNotFoundException {
        /* find */
        final Database database = databaseService.find(containerId, databaseId);
        final Table table = tableService.find(containerId, databaseId, tableId);
        /* run query */
        if (data.getData().size() == 0 || data.getKeys().size() == 0) return null;
        final Session session = getCurrentSession(database.getContainer().getImage(), database.getContainer(), database);
        /* prepare the statement */
        final InsertTableRawQuery raw = queryMapper.tableCsvDtoToRawUpdateQuery(table, data);
        final NativeQuery<?> query = session.createSQLQuery(raw.getQuery());
        final int[] idx = new int[]{0};
        data.getData()
                .forEach((key, value) -> query.setParameter(idx[0]++, value));
        log.trace("query with parameters {}", query);
        return execute(query, session);
    }

    @Override
    @Transactional
    public void delete(Long containerId, Long databaseId, Long tableId, TableCsvDeleteDto data)
            throws ImageNotSupportedException, TableMalformedException, DatabaseNotFoundException,
            TableNotFoundException, TupleDeleteException {
        /* find */
        final Database database = databaseService.find(containerId, databaseId);
        final Table table = tableService.find(containerId, databaseId, tableId);
        /* run query */
        if (data.getKeys().size() == 0) return;
        final Session session = getCurrentSession(database.getContainer().getImage(), database.getContainer(), database);
        final Transaction transaction = session.beginTransaction();
        /* prepare the statement */
        final NativeQuery<?> query = session.createSQLQuery(queryMapper.tableCsvDtoToRawDeleteQuery(table, data));
        final int[] idx = new int[]{0};
        data.getKeys()
                .forEach((key, value) -> query.setParameter(idx[0]++, value));
        final int affectedTuples;
        try {
            affectedTuples = query.executeUpdate();
            activeConnection(session);
            transaction.commit();
        } catch (PersistenceException e) {
            log.error("Failed to delete data");
            log.debug("failed to delete data: {}", e.getMessage());
            session.close();
            throw new TableMalformedException("Could not delete data", e);
        } finally {
            if (session.isOpen()) {
                session.close();
            }
        }
        if (affectedTuples == 0) {
            log.error("No tuples were deleted");
            throw new TupleDeleteException("No tuples deleted");
        }
        log.info("Deleted {} tuple(s)", affectedTuples);
        log.debug("Deleted tuple(s) {}", data);
    }

    @Override
    @Transactional
    public Integer insert(Long containerId, Long databaseId, Long tableId, ImportDto data)
            throws ImageNotSupportedException, TableMalformedException, DatabaseNotFoundException,
            TableNotFoundException, ContainerNotFoundException {
        /* find */
        final Database database = databaseService.find(containerId, databaseId);
        final Table table = tableService.find(containerId, databaseId, tableId);
        /* preparing the statements */
        final Session session1 = getCurrentSession(database.getContainer().getImage(), database.getContainer(), database);
        session1.beginTransaction();
        final Session session2 = getCurrentSession(database.getContainer().getImage(), database.getContainer(), database);
        session2.beginTransaction();
        final Session session3 = getCurrentSession(database.getContainer().getImage(), database.getContainer(), database);
        session3.beginTransaction();
        final Session session4 = getCurrentSession(database.getContainer().getImage(), database.getContainer(), database);
        session4.beginTransaction();

        /* Create a temporary table, insert there, transfer with update on duplicate key and lastly drops the temporary table */
        execute(session1.createSQLQuery(queryMapper.generateTemporaryTableSQL(table)), session1);
        execute(session2.createSQLQuery(queryMapper.pathToRawInsertQuery(table, data).getQuery()), session2);
        final Integer affectedTuples = execute(session3.createSQLQuery(queryMapper.generateInsertFromTemporaryTableSQL(table)), session3);
        execute(session4.createSQLQuery(queryMapper.dropTemporaryTableSQL(table)), session4);
        return affectedTuples;
    }

    /**
     * Executes a generic native query for a given session and factory
     *
     * @param query   The query.
     * @param session The session.
     * @return The number of affected tuples.
     * @throws TableMalformedException The table where the query was applied to is malformed.
     */
    private Integer execute(NativeQuery<?> query, Session session)
            throws TableMalformedException {
        final int affectedTuples;
        try {
            affectedTuples = query.executeUpdate();
            session.getTransaction()
                    .commit();
        } catch (PersistenceException e) {
            log.error("Could not insert data: {}", e.getMessage());
            session.close();
            throw new TableMalformedException("Could not insert data", e);
        } finally {
            if (session.isOpen()) {
                session.close();
            }
        }
        return affectedTuples;
    }

    /**
     * Parses the stored columns from a given query.
     *
     * @param query    The query.
     * @param database The database that contains the list of tables with list of columns.
     * @return List of columns in the order they are referenced in the query.
     * @throws JSQLParserException The columns could not be extracted from the query.
     */
    @Transactional(readOnly = true)
    protected List<TableColumn> parseColumns(Query query, Database database) throws JSQLParserException {
        final List<TableColumn> columns = new ArrayList<>();
        final CCJSqlParserManager parserRealSql = new CCJSqlParserManager();
        final Statement statement = parserRealSql.parse(new StringReader(query.getQuery()));

        /* check */
        if (!(statement instanceof Select)) {
            log.error("Query attempts to update the dataset, not a SELECT statement");
            throw new JSQLParserException("Query attempts to update the dataset");
        }

        /* start parsing */
        final Select selectStatement = (Select) statement;
        final PlainSelect ps = (PlainSelect) selectStatement.getSelectBody();
        final List<SelectItem> clauses = ps.getSelectItems();
        log.trace("columns referenced in the from-clause: {}", clauses);

        /* Parse all tables */
        final List<FromItem> tables = new ArrayList<>();
        tables.add(ps.getFromItem());
        if (ps.getJoins() != null && ps.getJoins().size() > 0) {
            log.trace("query contains join items: {}", ps.getJoins());
            for (Join j : ps.getJoins()) {
                if (j.getRightItem() != null) {
                    tables.add(j.getRightItem());
                }
            }
        }
        log.debug("tables referenced: {}", tables);
        log.trace("columns referenced in the from-clause and join-clause(s): {}", clauses);

        /* Checking if all tables exist */
        final List<TableColumn> allColumns = new ArrayList<>();
        for (FromItem fromItem : tables) {
            boolean i = false;
            for (final Table table : database.getTables()) {
                if (table.equals(fromItem)) {
                    log.trace("table {} equals from item {}", table.getInternalName(), fromItem);
                    allColumns.addAll(table.getColumns());
                    i = false;
                    break;
                }
                log.trace("table {} did not equal from item {}", table.getInternalName(), fromItem);
                i = true;
            }
            if (i) {
                log.error("Table {} does not exist", queryMapper.stringToEscapedString(fromItem.toString()));
                throw new JSQLParserException("Table does not exist");
            }
        }

        /* Checking if all columns exist */
        for (SelectItem item : clauses) {
            if (item.toString().trim().equals("*")) {
                log.error("Do not use * in queries");
                continue;
            }
            final String clause = queryMapper.selectItemToEscapedString(item);
            boolean i = false;
            for (TableColumn tc : allColumns) {
                if (tc.equals(item)) {
                    i = false;
                    columns.add(tc);
                    break;
                }
                i = true;
            }
            if (i) {
                log.error("Column {} does not exist", item);
                throw new JSQLParserException("Column does not exist");
            }
        }
        return columns;

    }

    /**
     * Counts the total number of tuples in the user database with given id for a given query object
     *
     * @param containerId The container id.
     * @param databaseId  The database id.
     * @param query       The query object.
     * @return The number of tuples this query returns.
     * @throws DatabaseNotFoundException  The user database was not found in the container.
     * @throws TableMalformedException    The table is malformed in the database (in the container).
     * @throws ImageNotSupportedException The database image is not supported.
     */
    @Transactional(readOnly = true)
    protected Long countQueryResults(Long containerId, Long databaseId, Query query)
            throws DatabaseNotFoundException, TableMalformedException, ImageNotSupportedException {
        /* find */
        final Database database = databaseService.find(containerId, databaseId);
        /* run query */
        final Session session = getCurrentSession(database.getContainer().getImage(), database.getContainer(), database);
        final Transaction transaction = session.beginTransaction();
        final NativeQuery<BigInteger> nativeQuery = session.createSQLQuery(
                queryMapper.queryToRawTimestampedCountQuery(query.getQuery(), database, query.getExecution()));
        final BigInteger result;
        try {
            log.debug("counted {} tuples from query {}", nativeQuery.executeUpdate(), query.getId());
            result = nativeQuery.getSingleResult();
            activeConnection(session);
            transaction.commit();
        } catch (PersistenceException e) {
            log.error("Failed to count tuples");
            session.close();
            throw new TableMalformedException("Failed to count tuples", e);
        } finally {
            if (session.isOpen()) {
                session.close();
            }
        }
        return result.longValue();
    }


}
