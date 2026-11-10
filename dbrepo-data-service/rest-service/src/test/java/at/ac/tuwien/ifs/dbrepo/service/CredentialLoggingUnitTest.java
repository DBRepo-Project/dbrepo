package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.config.DuckDbConfig;
import at.ac.tuwien.ifs.dbrepo.config.S3Config;
import at.ac.tuwien.ifs.dbrepo.core.api.database.AccessTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.User;
import at.ac.tuwien.ifs.dbrepo.core.exception.DatabaseMalformedException;
import at.ac.tuwien.ifs.dbrepo.core.exception.DatabaseUnavailableException;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.mapper.DuckDbMapper;
import at.ac.tuwien.ifs.dbrepo.mapper.MariaDbMapper;
import at.ac.tuwien.ifs.dbrepo.service.impl.AccessServiceMariaDbImpl;
import at.ac.tuwien.ifs.dbrepo.service.impl.AnalyseServiceDuckDbImpl;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.read.ListAppender;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.duckdb.DuckDBConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CredentialLoggingUnitTest extends BaseTest {
    private static final String PASSWORD = "credential-log-marker-unique";
    private static final String HASH = "*AABBCCDDEEFF00112233445566778899AABBCCDD";
    private final Logger logger = (Logger) LoggerFactory.getLogger("at.ac.tuwien.ifs.dbrepo");
    private final ListAppender<ILoggingEvent> captured = new ListAppender<>();
    private Level previous;

    @BeforeEach void capture() {
        previous = logger.getLevel();
        logger.setLevel(Level.TRACE);
        captured.start();
        logger.addAppender(captured);
    }

    @AfterEach void restore() {
        logger.detachAppender(captured);
        logger.setLevel(previous);
    }

    private void noCredentials(Throwable error) {
        StringBuilder text = new StringBuilder();
        captured.list.forEach(event -> text.append(event.getFormattedMessage())
                .append(event.getThrowableProxy() == null ? "" : ThrowableProxyUtil.asString(event.getThrowableProxy()))
                .append(event.getKeyValuePairs()));
        if (error != null) {
            StringWriter out = new StringWriter();
            error.printStackTrace(new PrintWriter(out));
            text.append(out);
        }
        assertFalse(text.toString().contains(PASSWORD));
        assertFalse(text.toString().contains(HASH));
    }

    @Test void credentialMappersDoNotLogTheirSqlOrParameterValues() {
        var duck = Mappers.getMapper(DuckDbMapper.class);
        var maria = Mappers.getMapper(MariaDbMapper.class);
        DATABASE_1_CACHE.getContainer().setPassword(PASSWORD);
        assertTrue(duck.databaseDtoToRawAttachQuery(DATABASE_1_CACHE).contains(PASSWORD));
        assertTrue(duck.queryToRawSetS3SecretQuery(PASSWORD, HASH).contains(HASH));
        assertTrue(duck.queryToRawSetVariableQuery("future_credential_setting", PASSWORD).contains(PASSWORD));
        assertTrue(maria.databaseSetPasswordQuery("fixture", HASH).contains(HASH));
        assertFalse(captured.list.isEmpty());
        noCredentials(null);
    }

    @Test void provisioningAndDriverFailuresKeepPlaintextAndHashesOutOfLogs() throws Exception {
        var pool = mock(ComboPooledDataSource.class);
        var connection = mock(Connection.class);
        var statement = mock(PreparedStatement.class);
        when(pool.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        var service = spy(new AccessServiceMariaDbImpl(Mappers.getMapper(MariaDbMapper.class)));
        doReturn(pool).when(service).getDataSource(DATABASE_1_CACHE);
        ReflectionTestUtils.setField(service, "grantDefaultRead", "SELECT");
        ReflectionTestUtils.setField(service, "grantDefaultWrite", "SELECT, INSERT");
        for (String password : new String[]{PASSWORD, HASH}) {
            service.create(DATABASE_1_CACHE, User.builder().username("fixture").password(password).build(), AccessTypeDto.READ);
        }
        when(statement.execute()).thenThrow(new SQLException(PASSWORD + HASH, "42000", 123));
        var error = assertThrows(DatabaseMalformedException.class, () -> service.create(DATABASE_1_CACHE,
                User.builder().username("fixture").password(PASSWORD).build(), AccessTypeDto.READ));
        noCredentials(error);
    }

    @Test void attachmentFailureCannotExposeCredentialSqlThroughItsExceptionChain() throws Exception {
        var connection = mock(DuckDBConnection.class);
        var statement = mock(PreparedStatement.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(connection.prepareStatement(startsWith("ATTACH ")))
                .thenThrow(new SQLException("ATTACH password=" + PASSWORD, "42000", 456));
        var s3 = mock(S3Config.class);
        when(s3.getS3aEndpoint()).thenReturn("http://fixture");
        when(s3.getS3aAccessKey()).thenReturn(PASSWORD);
        when(s3.getS3aSecretKey()).thenReturn(HASH);
        var config = mock(DuckDbConfig.class);
        var service = spy(new AnalyseServiceDuckDbImpl(s3, null, config,
                Mappers.getMapper(DuckDbMapper.class), null));
        doReturn(connection).when(service).getDuckDbConnection();
        DATABASE_1_CACHE.getContainer().setPassword(PASSWORD);
        var error = assertThrows(DatabaseUnavailableException.class,
                () -> service.determineDataTypes(DATABASE_1_CACHE, "SELECT 1"));
        noCredentials(error);
    }
}
