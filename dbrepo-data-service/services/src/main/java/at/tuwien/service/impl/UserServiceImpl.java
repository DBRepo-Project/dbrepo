package at.tuwien.service.impl;

import at.tuwien.api.user.UserBriefDto;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.user.User;
import at.tuwien.exception.QueryMalformedException;
import at.tuwien.exception.UserNotFoundException;
import at.tuwien.mapper.QueryMapper;
import at.tuwien.mapper.UserMapper;
import at.tuwien.repository.mdb.UserRepository;
import at.tuwien.service.UserService;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Log4j2
@Service
public class UserServiceImpl extends HibernateConnector implements UserService {

    private final UserMapper userMapper;
    private final QueryMapper queryMapper;
    private final UserRepository userRepository;

    @Autowired
    public UserServiceImpl(UserMapper userMapper, QueryMapper queryMapper, UserRepository userRepository) {
        this.userMapper = userMapper;
        this.queryMapper = queryMapper;
        this.userRepository = userRepository;
    }

    @Override
    public List<UserBriefDto> findAll(Database database) throws UserNotFoundException {
        /* run query */
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(database.getContainer().getImage(),
                database.getContainer(), database);
        try {
            final Connection connection = dataSource.getConnection();
            final PreparedStatement preparedStatement = prepareStatement(connection, queryMapper.findAllUsersQuery());
            final ResultSet resultSet = preparedStatement.executeQuery();
            return queryMapper.resultSetToUserDtoList(resultSet);
        } catch (SQLException | QueryMalformedException e) {
            log.error("Failed to find users from database with id {}: {}", database.getId(), e.getMessage());
            throw new UserNotFoundException("Failed to find users from database with id " + database.getId() + ": " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
    }

    @Override
    public User save(UserBriefDto data) {
        final User mapped = userMapper.userBriefDtoToUser(data);
        mapped.setEmailVerified(false);
        mapped.setEnabled(false);
        final User user = userRepository.save(mapped);
        log.info("Saved user with id {}", user.getId());
        return user;
    }

}
