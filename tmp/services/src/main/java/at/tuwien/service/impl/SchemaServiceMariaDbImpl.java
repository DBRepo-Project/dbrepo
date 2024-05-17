package at.tuwien.service.impl;

import at.tuwien.api.database.internal.PrivilegedDatabaseDto;
import at.tuwien.api.database.table.TableDto;
import at.tuwien.exception.QueryMalformedException;
import at.tuwien.mapper.MariaDbMapper;
import at.tuwien.mapper.MetadataMapper;
import at.tuwien.service.SchemaService;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@Log4j2
@Service
public class SchemaServiceMariaDbImpl extends HibernateConnector implements SchemaService {

    private final MariaDbMapper mariaDbMapper;
    private final MetadataMapper metadataMapper;

    @Autowired
    public SchemaServiceMariaDbImpl(MariaDbMapper mariaDbMapper, MetadataMapper metadataMapper) {
        this.mariaDbMapper = mariaDbMapper;
        this.metadataMapper = metadataMapper;
    }

    @Override
    public TableDto obtainTableMetadata(PrivilegedDatabaseDto database, String tableName) throws SQLException,
            QueryMalformedException {
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(database);
        final Connection connection = dataSource.getConnection();
        TableDto table;
        try {
            /* obtain basic table metadata */
            connection.commit();
            final PreparedStatement basicMetadataStatement = connection.prepareStatement("SELECT t.`TABLE_NAME`, t.`TABLE_TYPE`, t.`TABLE_ROWS`, t.`AVG_ROW_LENGTH`, t.`DATA_LENGTH`, t.`MAX_DATA_LENGTH`, COALESCE(t.`CREATE_TIME`, NOW()) as `CREATE_TIME`, t.`UPDATE_TIME`, v.`VIEW_DEFINITION` FROM information_schema.TABLES t LEFT JOIN information_schema.VIEWS v ON t.`TABLE_NAME` = v.`TABLE_NAME` WHERE t.`TABLE_SCHEMA` = ? AND t.`TABLE_TYPE` IN ('BASE TABLE', 'SYSTEM VERSIONED', 'VIEW') AND t.`TABLE_NAME` = ?");
            basicMetadataStatement.setString(1, database.getInternalName());
            basicMetadataStatement.setString(2, tableName);
            final TableDto tmp = mariaDbMapper.resultSetToTable(metadataMapper.privilegedDatabaseDtoToDatabaseDto(database), basicMetadataStatement.getResultSet());
            /* obtain table constraints metadata */
            final PreparedStatement constraintMetadataStatement = connection.prepareStatement("SELECT `ORDINAL_POSITION`, `COLUMN_DEFAULT`, `IS_NULLABLE`, `DATA_TYPE`, `CHARACTER_MAXIMUM_LENGTH`, `NUMERIC_PRECISION`, `NUMERIC_SCALE`, `COLUMN_TYPE`, `COLUMN_KEY`, `COLUMN_NAME` FROM `information_schema`.`COLUMNS` WHERE `TABLE_SCHEMA` = ? AND `TABLE_NAME` = ?;");
            constraintMetadataStatement.setString(1, database.getInternalName());
            constraintMetadataStatement.setString(2, tableName);
            table = mariaDbMapper.resultSetToTable(constraintMetadataStatement.getResultSet(), tmp,
                    database.getContainer().getDefaultDateFormat(), database.getContainer().getDefaultTimestampFormat());
        } finally {
            dataSource.close();
        }
        log.info("Obtained table metadata for table {}{}", database.getInternalName(), tableName);
        return table;
    }

}
