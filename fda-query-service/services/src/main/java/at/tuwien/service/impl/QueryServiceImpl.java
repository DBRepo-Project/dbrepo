package at.tuwien.service.impl;

import at.tuwien.InsertTableRawQuery;
import at.tuwien.api.database.query.ExecuteStatementDto;
import at.tuwien.api.database.query.ImportDto;
import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.api.database.table.TableCsvDto;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.database.table.columns.TableColumn;
import at.tuwien.exception.*;
import at.tuwien.mapper.QueryMapper;
import at.tuwien.querystore.Query;
import at.tuwien.repository.jpa.TableColumnRepository;
import at.tuwien.service.*;
import lombok.extern.log4j.Log4j2;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserManager;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.*;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.exception.SQLGrammarException;
import org.hibernate.query.NativeQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.PersistenceException;
import java.io.StringReader;
import java.math.BigInteger;
import java.sql.SQLException;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Log4j2
@Service
public class QueryServiceImpl extends HibernateConnector implements QueryService {

    private final QueryMapper queryMapper;
    private final StoreService storeService;
    private final TableService tableService;
    private final DatabaseService databaseService;
    private final CommaValueService commaValueService;

    @Autowired
    public QueryServiceImpl(QueryMapper queryMapper, TableService tableService, DatabaseService databaseService,
                            StoreService storeService, CommaValueService commaValueService) {
        this.queryMapper = queryMapper;
        this.storeService = storeService;
        this.tableService = tableService;
        this.databaseService = databaseService;
        this.commaValueService = commaValueService;
    }

    @Override
    @Transactional
    public QueryResultDto execute(Long containerId, Long databaseId, ExecuteStatementDto statement, Long page, Long size)
            throws DatabaseNotFoundException, ImageNotSupportedException, QueryMalformedException, QueryStoreException, ContainerNotFoundException, TableNotFoundException, SQLException, JSQLParserException, TableMalformedException {
        final Query q = storeService.insert(containerId, databaseId, null, statement, Instant.now());
        final QueryResultDto result = this.reExecute(containerId, databaseId, q, page, size);
        storeService.update(containerId, databaseId, result, result.getResultNumber(), q);
        return result;
    }

    @Override
    @Transactional
    public QueryResultDto reExecute(Long containerId, Long databaseId, Query query, Long page, Long size)
            throws QueryMalformedException, DatabaseNotFoundException, ImageNotSupportedException,
            SQLException, JSQLParserException, TableMalformedException {
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
        final NativeQuery<?> nativeQuery = session.createSQLQuery(queryMapper.queryToRawTimestampedQuery(
                query.getQuery(), database, query.getExecution(), page, size));
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
        final List<TableColumn> columns = parseColumns(query, database);
        QueryResultDto result = queryMapper.resultListToQueryResultDto(columns, nativeQuery.getResultList());
        result.setResultNumber(query.getResultNumber() != null ? query.getResultNumber() : countQueryResults(containerId, databaseId, query).longValue());
        result.setId(query.getId());
        session.close();
        factory.close();
        return result;
    }

    @Override
    @Transactional(readOnly = true)
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
        final NativeQuery<?> query = session.createSQLQuery(queryMapper.tableToRawFindAllQuery(table, timestamp, size,
                page));
        final int affectedTuples;
        try {
            final long startExec = System.currentTimeMillis();
            affectedTuples = query.executeUpdate();
            log.debug("executed query in {} ms", System.currentTimeMillis() - startExec);
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
        final NativeQuery<BigInteger> query = session.createSQLQuery(queryMapper.tableToRawCountAllQuery(table, timestamp));
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
        final InsertTableRawQuery raw = queryMapper.pathToRawInsertQuery(table, data);
        final NativeQuery<?> query = session.createSQLQuery(raw.getQuery());
        final Integer affectedTuples = insert(query, session, factory);
        /* delete csv dataset */
        commaValueService.delete(data.getLocation());
        return affectedTuples;
    }

    /**
     * Executes an insert query on an active Hibernate session on a table with given id and returns the affected rows.
     *
     * @param query   The query.
     * @param session The active Hibernate session.
     * @param factory The active Hibernate session factory.
     * @return The affected rows, if successful.
     * @throws TableMalformedException The table metadata is wrong.
     */
    private Integer insert(NativeQuery<?> query, Session session, SessionFactory factory) throws TableMalformedException {
        final int affectedTuples;
        try {
            affectedTuples = query.executeUpdate();
        } catch (PersistenceException e) {
            session.close();
            factory.close();
            log.error("Could not insert data: {}", e.getMessage());
            log.throwing(e);
            throw new TableMalformedException("Could not insert data", e);
        }
        session.getTransaction()
                .commit();
        session.close();
        factory.close();
        return affectedTuples;
    }

    /**
     * Parse table columns from the query structure and a given database
     *
     * @param query    The query.
     * @param database The database
     * @return The list of table columns.
     * @throws ImageNotSupportedException The container image is not supported.
     * @throws JSQLParserException        The query is invalid.
     */
    private List<TableColumn> parseColumns(Query query, Database database) throws ImageNotSupportedException,
            JSQLParserException {
        final List<TableColumn> columns = new ArrayList<>();
        final CCJSqlParserManager parserRealSql = new CCJSqlParserManager();
        final Statement statement = parserRealSql.parse(new StringReader(query.getQuery()));
        log.trace("given query {}", query.getQuery());

        if (statement instanceof Select) {
            Select selectStatement = (Select) statement;
            PlainSelect ps = (PlainSelect) selectStatement.getSelectBody();
            List<SelectItem> selectItems = ps.getSelectItems();

            /* parse all tables */
            List<FromItem> fromItems = new ArrayList<>();
            fromItems.add(ps.getFromItem());
            if (ps.getJoins() != null && ps.getJoins().size() > 0) {
                for (Join j : ps.getJoins()) {
                    if (j.getRightItem() != null) {
                        fromItems.add(j.getRightItem());
                    }
                }
            }
            /* checking if all tables exist */
            List<TableColumn> allColumns = new ArrayList<>();
            for (FromItem f : fromItems) {
                boolean i = false;
                log.trace("from item iterated through: {}", f);
                for (Table t : database.getTables()) {
                    if (queryMapper.stringToEscapedString(f.toString()).equals(queryMapper.stringToEscapedString(t.getInternalName()))) {
                        allColumns.addAll(t.getColumns());
                        i = false;
                        break;
                    }
                    i = true;
                }
                if (i) {
                    log.error("Table {} does not exist", f);
                    throw new JSQLParserException("Table does not exist");
                }
            }

            /* checking if all columns exist */
            for (SelectItem s : selectItems) {
                String select = queryMapper.stringToEscapedString(s.toString());
                log.debug(select);
                if (select.trim().equals("*")) {
                    log.warn("Please do not use * to query data");
                    continue;
                }
                /* ignore prefixes */
                if (select.contains(".")) {
                    log.debug(select);
                    select = select.split("\\.")[1];
                }
                boolean i = false;
                for (TableColumn tc : allColumns) {
                    log.trace("{},{},{}", tc.getInternalName(), tc.getName(), s);
                    if (select.equals(queryMapper.stringToEscapedString(tc.getInternalName()))) {
                        i = false;
                        columns.add(tc);
                        break;
                    }
                    i = true;
                }
                if (i) {
                    log.error("Column {} does not exist", s);
                    throw new JSQLParserException("Column does not exist");
                }
            }
            return columns;
        } else {
            log.error("SQL Query is not a SELECT statement - please only use SELECT statements");
            throw new JSQLParserException("Not a select statement");
        }

    }

    @Transactional(readOnly = true)
    protected BigInteger countQueryResults(Long containerId, Long databaseId, Query query)
            throws DatabaseNotFoundException, TableMalformedException, ImageNotSupportedException {
        /* find */
        final Database database = databaseService.find(databaseId);
        /* run query */
        final long startSession = System.currentTimeMillis();
        final SessionFactory factory = getSessionFactory(database, false);
        final Session session = factory.openSession();
        log.debug("opened hibernate session in {} ms", System.currentTimeMillis() - startSession);
        session.beginTransaction();
        final NativeQuery<BigInteger> nativeQuery = session.createSQLQuery(queryMapper.queryToRawTimestampedCountQuery(query.getQuery(), database, query.getExecution()));
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
        final BigInteger count = nativeQuery.getSingleResult();
        session.close();
        factory.close();
        return count;
    }


}
