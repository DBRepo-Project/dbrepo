package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.database.ViewDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints.ConstraintsDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.UserBriefDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.View;
import at.ac.tuwien.ifs.dbrepo.core.exception.DatabaseMalformedException;
import at.ac.tuwien.ifs.dbrepo.core.exception.QueryMalformedException;
import at.ac.tuwien.ifs.dbrepo.core.exception.ViewMalformedException;
import at.ac.tuwien.ifs.dbrepo.core.exception.ViewNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.i18n.Constants;
import at.ac.tuwien.ifs.dbrepo.core.mapper.MetadataMapper;
import at.ac.tuwien.ifs.dbrepo.mapper.DataMapper;
import at.ac.tuwien.ifs.dbrepo.mapper.MariaDbMapper;
import at.ac.tuwien.ifs.dbrepo.service.ViewService;
import com.google.common.hash.Hashing;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;

@Slf4j
@Service
public class ViewServiceMariaDbImpl extends DataConnector implements ViewService {

    private final DataMapper dataMapper;
    private final MariaDbMapper mariaDbMapper;
    private final MetadataMapper metadataMapper;

    @Autowired
    public ViewServiceMariaDbImpl(DataMapper dataMapper, MariaDbMapper mariaDbMapper, MetadataMapper metadataMapper) {
        this.dataMapper = dataMapper;
        this.mariaDbMapper = mariaDbMapper;
        this.metadataMapper = metadataMapper;
    }

    @Override
    public ViewDto create(Database database, String viewName, String query) throws SQLException,
            ViewMalformedException {
        final ComboPooledDataSource dataSource = getDataSource(database);
        final Connection connection = dataSource.getConnection();
        ViewDto view = ViewDto.builder()
                .name(viewName)
                .internalName(mariaDbMapper.nameToInternalName(viewName))
                .query(query)
                .queryHash(Hashing.sha256()
                        .hashString(query, StandardCharsets.UTF_8)
                        .toString())
                .isPublic(database.getIsPublic())
                .owner(UserBriefDto.builder()
                        .username(database.getOwnedBy())
                        .build())
                .identifiers(new LinkedList<>())
                .isInitialView(false)
                .databaseId(database.getId())
                .columns(new LinkedList<>())
                .build();
        try {
            /* create view if not exists */
            long start = System.currentTimeMillis();
            connection.prepareStatement(mariaDbMapper.viewCreateRawQuery(view.getInternalName(), query))
                    .execute();
            log.atDebug()
                    .setMessage("created view: " + view.getInternalName() + "." + database.getInternalName())
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "create_view")
                    .log();
            /* select view columns */
            start = System.currentTimeMillis();
            final PreparedStatement statement2 = connection.prepareStatement(mariaDbMapper.databaseTableColumnsSelectRawQuery());
            statement2.setString(1, database.getInternalName());
            statement2.setString(2, view.getInternalName());
            final ResultSet resultSet2 = statement2.executeQuery();
            log.atDebug()
                    .setMessage("created view: " + view.getInternalName() + "." + database.getInternalName())
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "select_view_columns")
                    .log();
            while (resultSet2.next()) {
                view = dataMapper.resultSetToTable(resultSet2, view);
            }
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            log.error("Failed to create view: {}", e.getMessage());
            throw new ViewMalformedException("Failed to create view: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        log.info("Created view with name {}", view.getName());
        return view;
    }

    @Override
    public List<ViewDto> explore(Database database) throws SQLException, DatabaseMalformedException,
            ViewNotFoundException {
        final ComboPooledDataSource dataSource = getDataSource(database);
        final Connection connection = dataSource.getConnection();
        final List<ViewDto> views = new LinkedList<>();
        try {
            /* inspect tables before views */
            final PreparedStatement statement = connection.prepareStatement(mariaDbMapper.databaseViewsSelectRawQuery());
            statement.setString(1, database.getInternalName());
            final long start = System.currentTimeMillis();
            final ResultSet resultSet1 = statement.executeQuery();
            log.atDebug()
                    .setMessage("explored views in database: " + database.getInternalName())
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "select_views")
                    .log();
            while (resultSet1.next()) {
                final String viewName = resultSet1.getString(1);
                if (viewName.length() == 64) {
                    log.trace("view {}.{} seems to be a subset view (name length = 64), skip.", database.getInternalName(), viewName);
                    continue;
                }
                if (database.getViews().stream().anyMatch(v -> v.getInternalName().equals(viewName))) {
                    log.trace("view {}.{} already known to metadata database, skip.", database.getInternalName(), viewName);
                    continue;
                }
                if (database.getTables().stream().noneMatch(t -> t.getInternalName().equals(viewName))) {
                    views.add(inspect(database, viewName));
                }
            }
        } catch (SQLException e) {
            log.error("Failed to get view schemas: {}", e.getMessage());
            throw new DatabaseMalformedException("Failed to get view schemas: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        log.info("Found {} view schema(s)", views.size());
        return views;
    }

    @Override
    public void delete(Database database, View view) throws SQLException, ViewMalformedException {
        final ComboPooledDataSource dataSource = getDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            /* drop view if exists */
            final long start = System.currentTimeMillis();
            connection.prepareStatement(mariaDbMapper.dropViewRawQuery(database.getInternalName(),
                            view.getInternalName()))
                    .execute();
            log.atDebug()
                    .setMessage("delete view: " + view.getInternalName() + "." + database.getInternalName())
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "view_delete")
                    .log();
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            log.error("Failed to delete view: {}", e.getMessage());
            throw new ViewMalformedException("Failed to delete view: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        log.info("Deleted view {}.{}", database.getInternalName(), view.getInternalName());
    }

    @Override
    @Timed(value = "dbrepo_data_count_view_data", description = "Time spent counting view data", histogram = true)
    public Long count(Database database, View view, Instant timestamp) throws SQLException,
            QueryMalformedException {
        final ComboPooledDataSource dataSource = getDataSource(database);
        final Connection connection = dataSource.getConnection();
        final Long queryResult;
        try {
            /* find view data */
            final long start = System.currentTimeMillis();
            final ResultSet resultSet = connection.prepareStatement(mariaDbMapper.selectCountRawQuery(
                            database.getInternalName(), view.getInternalName(), timestamp))
                    .executeQuery();
            log.atDebug()
                    .setMessage("count view: " + view.getInternalName() + "." + database.getInternalName())
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "count_view")
                    .log();
            queryResult = mariaDbMapper.resultSetToNumber(resultSet);
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            log.error("Failed to find row count from view {}.{}: {}", database.getInternalName(), view.getInternalName(), e.getMessage());
            throw new QueryMalformedException("Failed to find row count from view " + database.getInternalName() + "." + view.getInternalName() + ": " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        log.info("Find row count from view {}.{}", database.getInternalName(), view.getInternalName());
        return queryResult;
    }

    @Override
    public ViewDto inspect(Database database, String viewName) throws SQLException, ViewNotFoundException {
        final ComboPooledDataSource dataSource = getDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            /* obtain only view metadata */
            long start = System.currentTimeMillis();
            final PreparedStatement statement1 = connection.prepareStatement(mariaDbMapper.databaseTableSelectRawQuery());
            statement1.setString(1, database.getInternalName());
            statement1.setString(2, viewName);
            log.trace("1={}, 2={}", database.getInternalName(), viewName);
            final ResultSet resultSet1 = statement1.executeQuery();
            log.atDebug()
                    .setMessage("inspected view: " + viewName + "." + database.getInternalName())
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "select_view_schema")
                    .log();
            if (!resultSet1.next()) {
                throw new ViewNotFoundException("Failed to find view in the information schema");
            }
            final ViewDto view = dataMapper.schemaResultSetToView(database, resultSet1);
            view.setDatabaseId(database.getId());
            view.setOwner(UserBriefDto.builder()
                    .username(database.getOwnedBy())
                    .build());
            /* obtain view columns */
            start = System.currentTimeMillis();
            final PreparedStatement statement2 = connection.prepareStatement(mariaDbMapper.databaseTableColumnsSelectRawQuery());
            statement2.setString(1, database.getInternalName());
            statement2.setString(2, view.getInternalName());
            log.trace("1={}, 2={}", database.getInternalName(), view.getInternalName());
            final ResultSet resultSet2 = statement2.executeQuery();
            log.atDebug()
                    .setMessage("inspect view columns: " + viewName + "." + database.getInternalName())
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "select_view_columns")
                    .log();
            TableDto tmp = TableDto.builder()
                    .constraints(ConstraintsDto.builder()
                            .build())
                    .columns(new LinkedList<>())
                    .build();
            while (resultSet2.next()) {
                tmp = dataMapper.resultSetToTable(resultSet2, tmp);
            }
            view.setColumns(tmp.getColumns()
                    .stream()
                    .map(metadataMapper::columnDtoToViewColumnDto)
                    .toList());
            view.getColumns()
                    .forEach(column -> column.setDatabaseId(database.getId()));
            log.debug("obtained metadata for view {}.{}", database.getInternalName(), view.getInternalName());
            return view;
        } finally {
            dataSource.close();
        }
    }

}
