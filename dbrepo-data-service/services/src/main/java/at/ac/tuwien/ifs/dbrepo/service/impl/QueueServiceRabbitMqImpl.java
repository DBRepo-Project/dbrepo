package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.ColumnDto;
import at.ac.tuwien.ifs.dbrepo.mapper.DataMapper;
import at.ac.tuwien.ifs.dbrepo.service.QueueService;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class QueueServiceRabbitMqImpl extends DataConnector implements QueueService {

    private final DataMapper dataMapper;

    @Autowired
    public QueueServiceRabbitMqImpl(DataMapper dataMapper) {
        this.dataMapper = dataMapper;
    }

    @Override
    public void insert(DatabaseDto database, TableDto table, Map<String, Object> data) throws SQLException {
        final ComboPooledDataSource dataSource = getDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            final int[] idx = new int[]{1};
            final PreparedStatement preparedStatement = connection.prepareStatement(
                    dataMapper.rabbitMqTupleToInsertOrUpdateQuery(database.getInternalName(),
                            dataMapper.tableDtoToTableDto(table), data));
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                final Optional<ColumnDto> optional = table.getColumns().stream().filter(c -> c.getInternalName().equals(entry.getKey())).findFirst();
                if (optional.isEmpty()) {
                    log.error("Failed to find column with name {} in table with name {}, available columns are {}", entry.getKey(), table.getInternalName(), table.getColumns().stream().map(ColumnDto::getInternalName).toList());
                    continue;
                }
                dataMapper.prepareStatementWithColumnTypeObject(preparedStatement, optional.get().getColumnType(), idx[0]++,
                        entry.getValue());
            }
            final long start = System.currentTimeMillis();
            preparedStatement.executeUpdate();
            log.trace(EXECUTED_STATEMENT_MS, System.currentTimeMillis() - start);
            log.trace("successfully inserted tuple");
        } finally {
            dataSource.close();
        }
    }

}
