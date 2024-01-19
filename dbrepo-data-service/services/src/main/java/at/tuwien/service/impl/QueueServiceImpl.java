package at.tuwien.service.impl;

import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.table.Table;
import at.tuwien.exception.DatabaseNotFoundException;
import at.tuwien.exception.TableNotFoundException;
import at.tuwien.mapper.DataMapper;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.QueueService;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;

@Log4j2
@Service
public class QueueServiceImpl extends HibernateConnector implements QueueService {

    private final DataMapper dataMapper;
    private final DatabaseService databaseService;

    @Autowired
    public QueueServiceImpl(DataMapper dataMapper, DatabaseService databaseService) {
        this.dataMapper = dataMapper;
        this.databaseService = databaseService;
    }

    @Override
    @Transactional(readOnly = true)
    public void insert(String databaseInternalName, String tableInternalName, Map<String, Object> data)
            throws DatabaseNotFoundException, TableNotFoundException, SQLException {
        final Database database = databaseService.findByInternalName(databaseInternalName);
        log.debug("found database with id {} for name {}", database.getId(), databaseInternalName);
        final Optional<Table> optional = database.getTables()
                .stream()
                .filter(t -> t.getInternalName().equals(tableInternalName))
                .findFirst();
        if (optional.isEmpty()) {
            log.error("Failed to insert tuple into table {}: the table does not exist in database with name {}", tableInternalName, databaseInternalName);
            throw new TableNotFoundException("Failed to insert tuple into table " + tableInternalName + ": the table does not exist in database with name " + databaseInternalName);
        }
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(database.getContainer().getImage(),
                database.getContainer(), database);
        /* run query */
        try {
            final Connection connection = dataSource.getConnection();
            final PreparedStatement preparedStatement = dataMapper.rabbitMqTupleToInsertOrUpdateQuery(connection, optional.get(), data);
            preparedStatement.executeUpdate();
            log.trace("successfully inserted tuple");
        } finally {
            dataSource.close();
        }
    }

}
