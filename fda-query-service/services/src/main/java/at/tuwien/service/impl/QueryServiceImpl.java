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
import org.hibernate.SessionFactory;
import org.hibernate.exception.SQLGrammarException;
import org.hibernate.query.NativeQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.PersistenceException;
import javax.persistence.Tuple;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.math.BigInteger;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@Service
public class QueryServiceImpl extends HibernateConnector implements QueryService {

    private final QueryMapper queryMapper;
    private final TableService tableService;
    private final DatabaseService databaseService;
    private final StoreService storeService;

    @Autowired
    public QueryServiceImpl(QueryMapper queryMapper, TableService tableService, DatabaseService databaseService,
                            StoreService storeService) {
        this.queryMapper = queryMapper;
        this.tableService = tableService;
        this.databaseService = databaseService;
        this.storeService = storeService;
    }

    @Override
    @Transactional
    public QueryResultDto execute(Long containerId, Long databaseId, ExecuteStatementDto statement, Long page, Long size)
            throws DatabaseNotFoundException, ImageNotSupportedException, QueryMalformedException, QueryStoreException,
            ContainerNotFoundException, TableMalformedException, ColumnParseException {
        Instant i = Instant.now();
        Query q = storeService.insert(containerId, databaseId, null, statement, i);
        final QueryResultDto result = this.reExecute(containerId, databaseId, q, page, size);
        q = storeService.update(containerId, databaseId, result, result.getResultNumber(), q);
        return result;
    }

    @Override
    @Transactional
    public QueryResultDto reExecute(Long containerId, Long databaseId, Query query, Long page, Long size)
            throws QueryMalformedException, DatabaseNotFoundException, ImageNotSupportedException,
            TableMalformedException, ColumnParseException {
        /* find */
        final Database database = databaseService.find(databaseId);
        if (!database.getContainer().getImage().getRepository().equals("mariadb")) {
            throw new ImageNotSupportedException("Currently only MariaDB is supported");
        }
        /* run query */
        final long startSession = System.currentTimeMillis();
        final SessionFactory factory = getSessionFactory(database);
        final Session session = factory.openSession();
        log.debug("opened hibernate session in {} ms", System.currentTimeMillis() - startSession);
        session.beginTransaction();
        /* prepare the statement */
        final NativeQuery<?> nativeQuery = session.createSQLQuery(
                queryMapper.queryToRawTimestampedQuery(query.getQuery(), database, query.getExecution(), page, size));
        final int affectedTuples;
        try {
            log.debug("execute raw view-only query {}", query);
            affectedTuples = nativeQuery.executeUpdate();
            log.info("Execution on database id {} affected {} rows", databaseId, affectedTuples);
            session.getTransaction()
                    .commit();
        } catch (SQLGrammarException e) {
            session.close();
            factory.close();
            throw new QueryMalformedException("Query not valid for this database", e);
        }
        /* map the result to the tables (with respective columns) from the statement metadata */
        final List<TableColumn> columns;
        try {
            columns = parseColumns(query, database);
        } catch (JSQLParserException e) {
            log.error("Failed to map/parse columns.");
            throw new ColumnParseException("Failed to map/parse columns", e);
        }
        QueryResultDto result = queryMapper.resultListToQueryResultDto(columns, nativeQuery.getResultList());
        result.setResultNumber(
                query.getResultNumber() != null ? query.getResultNumber() : countQueryResults(containerId, databaseId,
                        query));
        result.setId(query.getId());
        session.close();
        factory.close();
        return result;
    }

    @Override
    @Transactional
    public QueryResultDto findAll(Long containerId, Long databaseId, Long tableId, Instant timestamp, Long page,
                                  Long size) throws TableNotFoundException, DatabaseNotFoundException,
            ImageNotSupportedException, DatabaseConnectionException, TableMalformedException, PaginationException,
            ContainerNotFoundException {
        /* find */
        final Database database = databaseService.find(databaseId);
        final Table table = tableService.find(databaseId, tableId);
        /* run query */
        final long startSession = System.currentTimeMillis();
        final SessionFactory factory = getSessionFactory(database, true);
        final Session session = factory.openSession();
        log.debug("opened hibernate session in {} ms", System.currentTimeMillis() - startSession);
        session.beginTransaction();
        final NativeQuery<?> query = session.createSQLQuery(
                queryMapper.tableToRawFindAllQuery(table, timestamp, size, page));
        final int affectedTuples;
        try {
            affectedTuples = query.executeUpdate();
            log.info("Found {} tuples in database id {}", affectedTuples, databaseId);
        } catch (PersistenceException e) {
            log.error("Failed to find data");
            session.close();
            factory.close();
            throw new TableMalformedException("Data not found", e);
        }
        session.getTransaction()
                .commit();
        final QueryResultDto result;
        try {
            result = queryMapper.queryTableToQueryResultDto(query.getResultList(), table);
        } catch (DateTimeException e) {
            log.error("Failed to parse date from the one stored in the metadata database");
            throw new TableMalformedException("Could not parse date from format", e);
        }
        session.close();
        factory.close();
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public ExportResource findAll(Long containerId, Long databaseId, Long tableId, Instant timestamp)
            throws TableNotFoundException, DatabaseNotFoundException, ImageNotSupportedException,
            DatabaseConnectionException, TableMalformedException, PaginationException, ContainerNotFoundException,
            FileStorageException {
        /* find */
        final Database database = databaseService.find(databaseId);
        final Table table = tableService.find(databaseId, tableId);
        /* run query */
        final long startSession = System.currentTimeMillis();
        final SessionFactory factory = getSessionFactory(database, true);
        final Session session = factory.openSession();
        final String filename = RandomStringUtils.randomAlphabetic(40) + ".csv";
        log.debug("opened hibernate session in {} ms", System.currentTimeMillis() - startSession);
        session.beginTransaction();
        final NativeQuery<?> query = session.createSQLQuery(
                queryMapper.tableToRawExportQuery(table, timestamp, filename));
        try {
            query.executeUpdate();
        } catch (PersistenceException e) {
            log.error("Failed to export table");
            session.close();
            factory.close();
            throw new TableMalformedException("Data not found", e);
        }
        session.getTransaction().commit();
        session.close();
        factory.close();
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
            ContainerNotFoundException, FileStorageException, QueryStoreException, QueryNotFoundException {
        /* find */
        final Database database = databaseService.find(databaseId);
        final Query query = storeService.findOne(containerId, databaseId, queryId);
        /* run query */
        final long startSession = System.currentTimeMillis();
        final SessionFactory factory = getSessionFactory(database, true);
        final Session session = factory.openSession();
        final String filename = RandomStringUtils.randomAlphabetic(40) + ".csv";
        log.debug("opened hibernate session in {} ms", System.currentTimeMillis() - startSession);
        session.beginTransaction();
        final NativeQuery<?> query2 = session.createSQLQuery(queryMapper.queryToRawExportQuery(query, filename));
        try {
            query2.executeUpdate();
        } catch (PersistenceException e) {
            log.error("Failed to export query");
            session.close();
            factory.close();
            throw new TableMalformedException("Data not found", e);
        }
        session.getTransaction().commit();
        session.close();
        factory.close();
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
    @Transactional
    public BigInteger count(Long containerId, Long databaseId, Long tableId, Instant timestamp)
            throws DatabaseNotFoundException, TableNotFoundException,
            TableMalformedException, ImageNotSupportedException {
        /* find */
        final Database database = databaseService.find(databaseId);
        final Table table = tableService.find(databaseId, tableId);
        /* run query */
        final long startSession = System.currentTimeMillis();
        final SessionFactory factory = getSessionFactory(database, false);
        final Session session = factory.openSession();
        log.debug("opened hibernate session in {} ms", System.currentTimeMillis() - startSession);
        session.beginTransaction();
        final NativeQuery<BigInteger> query = session.createSQLQuery(
                queryMapper.tableToRawCountAllQuery(table, timestamp));
        final int affectedTuples;
        try {
            affectedTuples = query.executeUpdate();
            log.info("Counted {} tuples in table id {}", affectedTuples, tableId);
        } catch (PersistenceException e) {
            log.error("Failed to count tuples");
            session.close();
            factory.close();
            throw new TableMalformedException("Data not found", e);
        }
        session.getTransaction()
                .commit();
        final BigInteger count = query.getSingleResult();
        session.close();
        factory.close();
        return count;
    }

    @Override
    @Transactional
    public Integer insert(Long containerId, Long databaseId, Long tableId, TableCsvDto data)
            throws ImageNotSupportedException, TableMalformedException, DatabaseNotFoundException,
            TableNotFoundException, ContainerNotFoundException {
        /* find */
        final Database database = databaseService.find(databaseId);
        final Table table = tableService.find(databaseId, tableId);
        /* run query */
        if (data.getData().size() == 0) return null;
        final long startSession = System.currentTimeMillis();
        final SessionFactory factory = getSessionFactory(database, true);
        final Session session = factory.openSession();
        log.debug("opened hibernate session in {} ms", System.currentTimeMillis() - startSession);
        session.beginTransaction();
        /* prepare the statement */
        final InsertTableRawQuery raw = queryMapper.tableCsvDtoToRawInsertQuery(table, data);
        final NativeQuery<?> query = session.createSQLQuery(raw.getQuery());

        log.trace("query with parameters {}", query.setParameterList(1, raw.getData()));
        return insert(query, session, factory);
    }

    @Override
    @Transactional
    public Integer update(Long containerId, Long databaseId, Long tableId, TableCsvUpdateDto data)
            throws ImageNotSupportedException, TableMalformedException, DatabaseNotFoundException,
            TableNotFoundException {
        /* find */
        final Database database = databaseService.find(databaseId);
        final Table table = tableService.find(databaseId, tableId);
        /* run query */
        if (data.getData().size() == 0 || data.getKeys().size() == 0) return null;
        final long startSession = System.currentTimeMillis();
        final SessionFactory factory = getSessionFactory(database, true);
        final Session session = factory.openSession();
        log.debug("opened hibernate session in {} ms", System.currentTimeMillis() - startSession);
        session.beginTransaction();
        /* prepare the statement */
        final InsertTableRawQuery raw = queryMapper.tableCsvDtoToRawUpdateQuery(table, data);
        final NativeQuery<?> query = session.createSQLQuery(raw.getQuery());
        final int[] idx = new int[]{0};
        data.getData()
                .forEach((key, value) -> query.setParameter(idx[0]++, value));
        log.trace("query with parameters {}", query);
        return insert(query, session, factory);
    }

    @Override
    @Transactional
    public void delete(Long containerId, Long databaseId, Long tableId, TableCsvDeleteDto data)
            throws ImageNotSupportedException, TableMalformedException, DatabaseNotFoundException,
            TableNotFoundException, TupleDeleteException {
        /* find */
        final Database database = databaseService.find(databaseId);
        final Table table = tableService.find(databaseId, tableId);
        /* run query */
        if (data.getKeys().size() == 0) return;
        final long startSession = System.currentTimeMillis();
        final SessionFactory factory = getSessionFactory(database, true);
        final Session session = factory.openSession();
        log.debug("opened hibernate session in {} ms", System.currentTimeMillis() - startSession);
        session.beginTransaction();
        /* prepare the statement */
        final NativeQuery<?> query = session.createSQLQuery(queryMapper.tableCsvDtoToRawDeleteQuery(table, data));
        final int[] idx = new int[]{0};
        data.getKeys()
                .forEach((key, value) -> query.setParameter(idx[0]++, value));
        final int affectedTuples;
        try {
            affectedTuples = query.executeUpdate();
        } catch (PersistenceException e) {
            session.close();
            factory.close();
            log.error("Could not insert data: {}", e.getMessage());
            throw new TableMalformedException("Could not insert data", e);
        }
        session.getTransaction()
                .commit();
        session.close();
        factory.close();
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
        final Database database = databaseService.find(databaseId);
        final Table table = tableService.find(databaseId, tableId);
        /* run query */
        final long startSession = System.currentTimeMillis();
        final SessionFactory factory = getSessionFactory(database, true);
        final Session session = factory.openSession();
        log.debug("opened hibernate session in {} ms", System.currentTimeMillis() - startSession);
        session.beginTransaction();
        /* prepare the statement */
        final String rawTemp = queryMapper.generateTemporaryTableSQL(table);
        final NativeQuery<?> queryCreate = session.createSQLQuery(rawTemp);
        log.debug(rawTemp);
        final String rawDeleteTemp = queryMapper.dropTemporaryTableSQL(table);
        final NativeQuery<?> queryDelete = session.createSQLQuery(rawDeleteTemp);
        log.debug(rawDeleteTemp);
        insert(queryCreate,session, factory);
        final InsertTableRawQuery raw = queryMapper.pathToRawInsertQuery(table, data);
        final NativeQuery<?> query = session.createSQLQuery(raw.getQuery());
        Integer i = insert(query, session, factory);
        insert(queryDelete, session, factory);
        session.close();
        factory.close();
        return i;
    }

    /**
     * Executes a insert query on an active Hibernate session on a table with given id and returns the affected rows.
     *
     * @param query   The query.
     * @param session The active Hibernate session.
     * @param factory The active Hibernate session factory.
     * @return The affected rows, if successful.
     * @throws TableMalformedException The table metadata is wrong.
     */
    private Integer insert(NativeQuery<?> query, Session session, SessionFactory factory)
            throws TableMalformedException {
        final int affectedTuples;
        try {
            affectedTuples = query.executeUpdate();
        } catch (PersistenceException e) {
            session.close();
            factory.close();
            log.error("Could not insert data: {}", e.getMessage());
            throw new TableMalformedException("Could not insert data", e);
        }
        session.getTransaction()
                .commit();
        return affectedTuples;
    }

    @Transactional(readOnly = true)
    protected List<TableColumn> parseColumns(Query query, Database database)
            throws ImageNotSupportedException, JSQLParserException {
        final List<TableColumn> columns = new ArrayList<>();

        final CCJSqlParserManager parserRealSql = new CCJSqlParserManager();
        final Statement statement = parserRealSql.parse(new StringReader(query.getQuery()));

        if (statement instanceof Select) {
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
            log.debug("columns referenced in the from-clause and join-clause(s): {}", clauses);

            /* Checking if all tables exist */
            List<TableColumn> allColumns = new ArrayList<>();
            for (FromItem fromItem : tables) {
                boolean i = false;
                for (Table table : database.getTables()) {
                    if (queryMapper.stringToEscapedString(table.getInternalName()).equals(
                            queryMapper.stringToEscapedString(fromItem.toString()))) {
                        allColumns.addAll(table.getColumns());
                        log.trace("matched table {} with columns {}", table.getInternalName(),
                                table.getColumns().stream().map(TableColumn::getInternalName).collect(
                                        Collectors.toList()));
                        i = false;
                        break;
                    }
                    i = true;
                }
                if (i) {
                    log.error("Table {} does not exist", queryMapper.stringToEscapedString(fromItem.toString()));
                    throw new JSQLParserException("Table does not exist");
                }
            }

            /* Checking if all columns exist */
            for (SelectItem clause : clauses) {
                String select = queryMapper.stringToEscapedString(clause.toString());
                log.debug(select);
                if (select.trim().equals("*")) {
                    log.warn("Do not use * in queries");
                    continue;
                }
                // ignore prefixes
                if (select.contains(".")) {
                    log.debug(select);
                    select = select.split("\\.")[1];
                }
                boolean i = false;
                for (TableColumn tc : allColumns) {
                    log.trace("{},{},{}", tc.getInternalName(), tc.getName(), clause);
                    if (select.equals(queryMapper.stringToEscapedString(tc.getInternalName()))) {
                        i = false;
                        columns.add(tc);
                        break;
                    }
                    i = true;
                }
                if (i) {
                    log.error("Column {} does not exist", clause);
                    throw new JSQLParserException("Column does not exist");
                }
            }
            return columns;
        } else {
            log.error("Query attempts to update the dataset, not a SELECT statement");
            throw new JSQLParserException("Query attempts to update the dataset");
        }

    }

    /**
     * mw: isn't this highly ineffective? We already have a {@link #count(Long, Long, Long, Instant)}  function
     */
    @Transactional
    protected Long countQueryResults(Long containerId, Long databaseId, Query query)
            throws DatabaseNotFoundException, TableMalformedException, ImageNotSupportedException {
        /* find */
        final Database database = databaseService.find(databaseId);
        /* run query */
        final long startSession = System.currentTimeMillis();
        final SessionFactory factory = getSessionFactory(database, false);
        final Session session = factory.openSession();
        log.debug("opened hibernate session in {} ms", System.currentTimeMillis() - startSession);
        session.beginTransaction();
        final NativeQuery<BigInteger> nativeQuery = session.createSQLQuery(
                queryMapper.queryToRawTimestampedCountQuery(query.getQuery(), database, query.getExecution()));
        final int affectedTuples;
        try {
            affectedTuples = nativeQuery.executeUpdate();
            log.info("Counted {} tuples from query {}", affectedTuples, query.getId());
        } catch (PersistenceException e) {
            log.error("Failed to count tuples");
            session.close();
            factory.close();
            throw new TableMalformedException("Data not found", e);
        }
        session.getTransaction()
                .commit();
        final Long count = nativeQuery.getSingleResult().longValue();
        session.close();
        factory.close();
        return count;
    }


}
