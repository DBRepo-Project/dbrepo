package at.tuwien.service.impl;

import at.tuwien.entities.database.Database;
import at.tuwien.exception.DatabaseNotFoundException;
import at.tuwien.exception.ImageNotSupportedException;
import at.tuwien.exception.QueryStoreException;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.QueryStoreService;
import lombok.extern.log4j.Log4j2;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.PersistenceException;

@Log4j2
@Service
public class QueryStoreServiceImpl extends HibernateConnector implements QueryStoreService {

    private final DatabaseService databaseService;

    @Autowired
    public QueryStoreServiceImpl(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    @Override
    public void create(Long containerId, Long databaseId) throws DatabaseNotFoundException,
            ImageNotSupportedException, QueryStoreException {
        /* find */
        final Database database = databaseService.findById(containerId, databaseId);
        if (!database.getContainer().getImage().getRepository().equals("mariadb")) {
            throw new ImageNotSupportedException("Currently only MariaDB is supported");
        }
        /* run query */
        final Session session = getCurrentSession(database.getContainer().getImage(), database.getContainer(), database);
        final Transaction transaction = session.beginTransaction();
        /* use jpq to select all */
        final org.hibernate.query.Query<at.tuwien.querystore.Query> queries = session.createQuery("select q from Query q",
                at.tuwien.querystore.Query.class);
        try {
            queries.getResultList();
            activeConnection(session);
            transaction.commit();
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
}
