package at.tuwien.service.impl;

import at.tuwien.api.database.DatabaseBriefDto;
import at.tuwien.entities.container.Container;
import at.tuwien.entities.database.Database;
import at.tuwien.exception.ContainerNotFoundException;
import at.tuwien.exception.DatabaseNotFoundException;
import at.tuwien.exception.QueryMalformedException;
import at.tuwien.mapper.DatabaseMapper;
import at.tuwien.mapper.QueryMapper;
import at.tuwien.repository.mdb.DatabaseRepository;
import at.tuwien.service.DatabaseService;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jpa.JpaObjectRetrievalFailureException;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Log4j2
@Service
public class DatabaseServiceImpl extends HibernateConnector implements DatabaseService {

    private final QueryMapper queryMapper;
    private final DatabaseMapper databaseMapper;
    private final DatabaseRepository databaseRepository;

    @Autowired
    public DatabaseServiceImpl(QueryMapper queryMapper, DatabaseMapper databaseMapper,
                               DatabaseRepository databaseRepository) {
        this.queryMapper = queryMapper;
        this.databaseMapper = databaseMapper;
        this.databaseRepository = databaseRepository;
    }

    @Override
    public List<DatabaseBriefDto> findAll(Container container) throws DatabaseNotFoundException {
        final Database informationSchema = Database.builder()
                .internalName("INFORMATION_SCHEMA")
                .build();
        /* run query */
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(container.getImage(), container, informationSchema);
        try {
            final Connection connection = dataSource.getConnection();
            final PreparedStatement preparedStatement = prepareStatement(connection, queryMapper.findAllDatabasesQuery());
            final ResultSet resultSet = preparedStatement.executeQuery();
            return queryMapper.resultSetToDatabaseDtoList(resultSet);
        } catch (SQLException | QueryMalformedException e) {
            log.error("Failed to find databases from container with id {}: {}", container.getId(), e.getMessage());
            throw new DatabaseNotFoundException("Failed to find databases from container with id " + container.getId() + ": " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
    }

    @Override
    public Database save(DatabaseBriefDto data) throws ContainerNotFoundException {
        final Database mapped = databaseMapper.databaseBriefDtoToDatabase(data);
        try {
            final Database database = databaseRepository.save(mapped);
            log.info("Saved database with id {}", database.getId());
            return database;
        } catch (JpaObjectRetrievalFailureException e) {
            log.error("Failed to save database: {}", e.getMessage());
            throw new ContainerNotFoundException("Failed to save database: " + e.getMessage(), e);
        }
    }

}
