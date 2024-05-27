package at.tuwien.service.impl;

import at.tuwien.api.database.DatabaseDto;
import at.tuwien.api.database.ViewColumnDto;
import at.tuwien.api.database.ViewDto;
import at.tuwien.api.database.internal.PrivilegedDatabaseDto;
import at.tuwien.api.database.table.TableDto;
import at.tuwien.api.database.table.columns.ColumnDto;
import at.tuwien.api.database.table.constraints.unique.UniqueDto;
import at.tuwien.config.QueryConfig;
import at.tuwien.exception.TableNotFoundException;
import at.tuwien.exception.ViewMalformedException;
import at.tuwien.exception.ViewNotFoundException;
import at.tuwien.exception.ViewSchemaException;
import at.tuwien.mapper.MariaDbMapper;
import at.tuwien.mapper.MetadataMapper;
import at.tuwien.service.SchemaService;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

@Log4j2
@Service
public class SchemaServiceMariaDbImpl extends HibernateConnector implements SchemaService {

    private final QueryConfig queryConfig;
    private final MariaDbMapper mariaDbMapper;
    private final MetadataMapper metadataMapper;

    @Autowired
    public SchemaServiceMariaDbImpl(QueryConfig queryConfig, MariaDbMapper mariaDbMapper,
                                    MetadataMapper metadataMapper) {
        this.queryConfig = queryConfig;
        this.mariaDbMapper = mariaDbMapper;
        this.metadataMapper = metadataMapper;
    }

    @Override
    public TableDto inspectTable(PrivilegedDatabaseDto database, String tableName) throws SQLException,
            TableNotFoundException {
        log.trace("inspecting table: {}.{}", database.getInternalName(), tableName);
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            /* obtain only table metadata */
            final PreparedStatement statement1 = connection.prepareStatement(mariaDbMapper.databaseTableSelectRawQuery());
            statement1.setString(1, database.getInternalName());
            statement1.setString(2, tableName);
            log.trace("1={}, 2={}", database.getInternalName(), tableName);
            TableDto table = mariaDbMapper.schemaResultSetToTable(metadataMapper.privilegedDatabaseDtoToDatabaseDto(database), statement1.executeQuery());
            /* obtain columns metadata */
            final PreparedStatement statement2 = connection.prepareStatement(mariaDbMapper.databaseTableColumnsSelectRawQuery());
            statement2.setString(1, database.getInternalName());
            statement2.setString(2, tableName);
            log.trace("1={}, 2={}", database.getInternalName(), tableName);
            final ResultSet resultSet2 = statement2.executeQuery();
            while (resultSet2.next()) {
                table = mariaDbMapper.resultSetToTable(resultSet2, table, queryConfig);
            }
            /* obtain check constraints metadata */
            final PreparedStatement statement3 = connection.prepareStatement(mariaDbMapper.columnsCheckConstraintSelectRawQuery());
            statement3.setString(1, database.getInternalName());
            statement3.setString(2, tableName);
            log.trace("1={}, 2={}", database.getInternalName(), tableName);
            final ResultSet resultSet3 = statement3.executeQuery();
            while (resultSet3.next()) {
                final String clause = resultSet3.getString(1);
                table.getConstraints()
                        .getChecks()
                        .add(clause);
                log.trace("found check clause: {}", clause);
            }
            /* obtain column constraints metadata */
            final PreparedStatement statement4 = connection.prepareStatement(mariaDbMapper.databaseTableConstraintsSelectRawQuery());
            statement4.setString(1, database.getInternalName());
            statement4.setString(2, tableName);
            log.trace("1={}, 2={}", database.getInternalName(), tableName);
            final ResultSet resultSet4 = statement4.executeQuery();
            while (resultSet4.next()) {
                table = mariaDbMapper.resultSetToConstraint(resultSet4, table);
                for (UniqueDto uk : table.getConstraints().getUniques()) {
                    uk.setTable(metadataMapper.tableDtoToTableBriefDto(table));
                    final TableDto tmpTable = table;
                    uk.getColumns()
                            .forEach(column -> {
                                column.setTable(tmpTable);
                                column.setTableId(tmpTable.getId());
                                column.setDatabaseId(database.getId());
                                column.setIsPublic(database.getIsPublic());
                            });
                }
            }
            table.setTdbid(database.getId());
            table.setCreator(database.getCreator());
            table.setCreatedBy(database.getCreator().getId());
            final TableDto tmpTable = table;
            tmpTable.getColumns()
                    .forEach(column -> {
                        column.setTable(tmpTable);
                        column.setTableId(tmpTable.getId());
                        column.setDatabaseId(database.getId());
                    });
            log.debug("obtained metadata for table {}.{}", database.getInternalName(), tableName);
            return tmpTable;
        } finally {
            dataSource.close();
        }
    }

    @Override
    public ViewDto inspectView(PrivilegedDatabaseDto privilegedDatabase, String viewName) throws SQLException,
            ViewNotFoundException {
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(privilegedDatabase);
        final Connection connection = dataSource.getConnection();
        final DatabaseDto database = metadataMapper.privilegedDatabaseDtoToDatabaseDto(privilegedDatabase);
        try {
            /* obtain only view metadata */
            final PreparedStatement statement1 = connection.prepareStatement(mariaDbMapper.databaseViewSelectRawQuery());
            statement1.setString(1, database.getInternalName());
            statement1.setString(2, viewName);
            log.trace("1={}, 2={}", database.getInternalName(), viewName);
            final ResultSet resultSet1 = statement1.executeQuery();
            if (!resultSet1.next()) {
                throw new ViewNotFoundException("Failed to find view in the information schema");
            }
            ViewDto view = mariaDbMapper.schemaResultSetToView(metadataMapper.privilegedDatabaseDtoToDatabaseDto(privilegedDatabase), resultSet1);
            view.setDatabase(database);
            view.setVdbid(database.getId());
            view.setCreator(database.getCreator());
            view.setCreatedBy(database.getCreator().getId());
            /* obtain view columns */
            final PreparedStatement statement2 = connection.prepareStatement(mariaDbMapper.databaseTableColumnsSelectRawQuery());
            statement2.setString(1, database.getInternalName());
            statement2.setString(2, viewName);
            log.trace("1={}, 2={}", database.getInternalName(), viewName);
            final ResultSet resultSet2 = statement2.executeQuery();
            TableDto tmp = TableDto.builder()
                    .columns(new LinkedList<>())
                    .build();
            while (resultSet2.next()) {
                tmp = mariaDbMapper.resultSetToTable(resultSet2, tmp, queryConfig);
            }
            view.setColumns(tmp.getColumns()
                    .stream()
                    .map(metadataMapper::columnDtoToViewColumnDto)
                    .toList());
            view.getColumns()
                    .forEach(column -> column.setDatabaseId(database.getId()));
            log.debug("obtained metadata for view {}.{}", database.getInternalName(), viewName);
            return view;
        } finally {
            dataSource.close();
        }
    }

}
