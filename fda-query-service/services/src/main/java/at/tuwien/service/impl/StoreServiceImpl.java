package at.tuwien.service.impl;

import at.tuwien.api.database.query.ExecuteStatementDto;
import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.api.database.query.SaveStatementDto;
import at.tuwien.entities.user.User;
import at.tuwien.entities.database.Database;
import at.tuwien.exception.*;
import at.tuwien.mapper.QueryMapper;
import at.tuwien.mapper.StoreMapper;
import at.tuwien.querystore.Query;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.StoreService;
import at.tuwien.service.UserService;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.digest.DigestUtils;
import org.hibernate.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.PersistenceException;
import java.security.Principal;
import java.time.Instant;
import java.util.List;

@Log4j2
@Service
public class StoreServiceImpl extends HibernateConnector implements StoreService {

    private final QueryMapper queryMapper;
    private final StoreMapper storeMapper;
    private final UserService userService;
    private final DatabaseService databaseService;

    @Autowired
    public StoreServiceImpl(QueryMapper queryMapper, StoreMapper storeMapper, UserService userService,
                            DatabaseService databaseService) {
        this.queryMapper = queryMapper;
        this.storeMapper = storeMapper;
        this.userService = userService;
        this.databaseService = databaseService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<at.tuwien.querystore.Query> findAll(Long containerId, Long databaseId) throws DatabaseNotFoundException,
            ImageNotSupportedException, QueryStoreException, ContainerNotFoundException {
        /* find */
        final Database database = databaseService.find(containerId, databaseId);
        if (!database.getContainer().getImage().getRepository().equals("mariadb")) {
            throw new ImageNotSupportedException("Currently only MariaDB is supported");
        }
        log.trace("find all queries in database id {}", databaseId);
        /* run query */
        final Session session = getCurrentSession(database.getContainer().getImage(), database.getContainer(), database);
        final Transaction transaction = session.beginTransaction();
        activeConnection(session);
        /* use jpq to select all */
        final org.hibernate.query.Query<at.tuwien.querystore.Query> queries = session.createQuery("select q from Query q",
                at.tuwien.querystore.Query.class);
        final List<Query> out;
        try {
            out = queries.list();
            transaction.commit();
            return out;
        } catch (PersistenceException e) {
            log.error("Failed to find all queries");
            session.close();
            throw new QueryStoreException("Failed to find all queries");
        } finally {
            if (session.isOpen()) {
                session.close();
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public at.tuwien.querystore.Query findOne(Long containerId, Long databaseId, Long queryId) throws DatabaseNotFoundException,
            ImageNotSupportedException, QueryNotFoundException, QueryStoreException {
        /* find */
        final Database database = databaseService.find(containerId, databaseId);
        if (!database.getContainer().getImage().getRepository().equals("mariadb")) {
            throw new ImageNotSupportedException("Currently only MariaDB is supported");
        }
        /* run query */
        final Session session = getCurrentSession(database.getContainer().getImage(), database.getContainer(), database);
        final Transaction transaction = session.beginTransaction();
        /* use jpa to select one */
        final org.hibernate.query.Query<at.tuwien.querystore.Query> query = session.createQuery(
                "from Query where cid = :cid and dbid = :dbid and id = :id",
                at.tuwien.querystore.Query.class);
        query.setParameter("cid", containerId);
        query.setParameter("dbid", databaseId);
        query.setParameter("id", queryId);
        final at.tuwien.querystore.Query result;
        try {
            result = query.uniqueResult();
            activeConnection(session);
            transaction.commit();
        } catch (PersistenceException e) {
            log.error("Failed to find single query");
            session.close();
            throw new QueryStoreException("Failed to find single query", e);
        } finally {
            if (session.isOpen()) {
                session.close();
            }
        }
        if (result == null) {
            log.error("Query not found with id {}", queryId);
            throw new QueryNotFoundException("Query not found");
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public at.tuwien.querystore.Query insert(Long containerId, Long databaseId, QueryResultDto result, SaveStatementDto metadata,
                                             Principal principal)
            throws QueryStoreException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException, UserNotFoundException {
        return insert(containerId, databaseId, result, queryMapper.saveStatementDtoToExecuteStatementDto(metadata),
                principal, null);
    }

    @Override
    @Transactional(readOnly = true)
    public at.tuwien.querystore.Query insert(Long containerId, Long databaseId, QueryResultDto result, ExecuteStatementDto metadata,
                                             Principal principal, Instant execution)
            throws QueryStoreException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException, UserNotFoundException {
        /* find */
        final Database database = databaseService.find(containerId, databaseId);
        if (!database.getContainer().getImage().getRepository().equals("mariadb")) {
            throw new ImageNotSupportedException("Currently only MariaDB is supported");
        }
        log.debug("Insert into database id {}, metadata {}", databaseId, metadata);
        /* user */
        final User creator = userService.findByUsername(principal.getName());
        /* save */
        final Session session = getCurrentSession(database.getContainer().getImage(), database.getContainer(), database);
        final Transaction transaction = session.beginTransaction();
        final at.tuwien.querystore.Query query = at.tuwien.querystore.Query.builder()
                .cid(containerId)
                .dbid(databaseId)
                .query(metadata.getStatement())
                .queryNormalized(metadata.getStatement())
                .queryHash(DigestUtils.sha256Hex(metadata.getStatement()))
                .resultNumber(storeMapper.queryResultDtoToLong(result))
                .resultHash(storeMapper.queryResultDtoToString(result))
                .execution(execution)
                .createdBy(creator.getId())
                .build();
        try {
            session.save(query);
            activeConnection(session);
            transaction.commit();
            /* store the result in the query store */
            log.info("Saved query with id {}", query.getId());
            log.debug("saved query {}", query);
            return query;
        } catch (PersistenceException e) {
            log.error("Failed to save query");
            session.close();
            throw new QueryStoreException("Failed to save query", e);
        } finally {
            if (session.isOpen()) {
                session.close();
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public at.tuwien.querystore.Query update(Long containerId, Long databaseId, QueryResultDto result, Long resultNumber,
                                             at.tuwien.querystore.Query query)
            throws DatabaseNotFoundException, ImageNotSupportedException, QueryStoreException {
        /* find */
        final Database database = databaseService.find(containerId, databaseId);
        if (!database.getContainer().getImage().getRepository().equals("mariadb")) {
            throw new ImageNotSupportedException("Currently only MariaDB is supported");
        }

        log.debug("Update database id {}, metadata {}", databaseId, query);
        /* save */
        final Session session = getCurrentSession(database.getContainer().getImage(), database.getContainer(), database);
        final Transaction transaction = session.beginTransaction();
        query.setQueryHash(DigestUtils.sha256Hex(query.getQuery()));
        query.setResultNumber(resultNumber);
        query.setResultHash(storeMapper.queryResultDtoToString(result));
        try {
            session.update(query);
            activeConnection(session);
            transaction.commit();
            /* store the result in the query store */
            log.info("Update query with id {}", query.getId());
            log.debug("saved query {}", query);
            return query;
        } catch (PersistenceException e) {
            log.error("Failed to update query");
            session.close();
            throw new QueryStoreException("Failed to update query", e);
        } finally {
            if (session.isOpen()) {
                session.close();
            }
        }
    }


}
