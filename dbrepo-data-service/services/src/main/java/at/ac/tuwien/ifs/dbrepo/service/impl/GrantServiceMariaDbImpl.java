package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseGrantsDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.User;
import at.ac.tuwien.ifs.dbrepo.core.exception.AccessNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.exception.DatabaseMalformedException;
import at.ac.tuwien.ifs.dbrepo.core.i18n.Constants;
import at.ac.tuwien.ifs.dbrepo.core.mapper.MetadataMapper;
import at.ac.tuwien.ifs.dbrepo.mapper.MariaDbMapper;
import at.ac.tuwien.ifs.dbrepo.service.GrantService;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class GrantServiceMariaDbImpl extends DataConnector implements GrantService {

    @Value("${dbrepo.grant.default.read}")
    private String grantDefaultRead;

    @Value("${dbrepo.grant.default.write}")
    private String grantDefaultWrite;

    private final MariaDbMapper mariaDbMapper;
    private final MetadataMapper metadataMapper;

    @Autowired
    public GrantServiceMariaDbImpl(MariaDbMapper mariaDbMapper, MetadataMapper metadataMapper) {
        this.mariaDbMapper = mariaDbMapper;
        this.metadataMapper = metadataMapper;
    }

    @Override
    public DatabaseGrantsDto find(Database database, User user) throws SQLException, DatabaseMalformedException,
            AccessNotFoundException {
        final Map<String, DatabaseGrantsDto> grants = findAll(database, user);
        String key = database.getInternalName();
        if (!grants.containsKey(key)) {
            key = "*";
            if (!grants.containsKey(key)) {
                log.atError()
                        .setMessage("Failed to find access grant(s) for database: " + database.getInternalName() + " or fallback key *")
                        .addKeyValue("user_id", user.getId())
                        .addKeyValue("database_id", database.getId())
                        .log();
                /* there must be at least 1 grant otherwise the user does not exist in the database which indicates malformed */
                throw new AccessNotFoundException("Failed to find access grant(s) for database: " + database.getInternalName() + " or fallback key *");
            }
        }
        final DatabaseGrantsDto grant = grants.get(key);
        log.debug("found grant: {}", grant);
        return grant;
    }

    @Override
    public Map<String, DatabaseGrantsDto> findAll(Database database, User user) throws SQLException,
            DatabaseMalformedException {
        final ComboPooledDataSource dataSource = getDataSource(database);
        final Connection connection = dataSource.getConnection();
        final Map<String, DatabaseGrantsDto> grants = new HashMap<>();
        try {
            /* get access */
            long start = System.currentTimeMillis();
            final PreparedStatement statement = connection.prepareStatement(mariaDbMapper.databaseFindAccessQuery());
            statement.setString(1, user.getUsername());
            log.trace("1={}", user.getUsername());
            final ResultSet resultSet = statement.executeQuery();
            log.atDebug()
                    .setMessage("found user " + user.getUsername() + " grant(s) in database(s): " + grants.keySet())
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "list_grants")
                    .log();
            while (resultSet.next()) {
                mariaDbMapper.resultSetToGrants(resultSet)
                        .forEach((k, v) -> grants.put(k,
                                metadataMapper.grantsToDatabaseGrantDto(v, grantDefaultRead, grantDefaultWrite)));
            }
            return grants;
        } catch (SQLException e) {
            connection.rollback();
            log.error("Failed to list database access: {}", e.getMessage());
            throw new DatabaseMalformedException("Failed to list database access: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
    }
}
