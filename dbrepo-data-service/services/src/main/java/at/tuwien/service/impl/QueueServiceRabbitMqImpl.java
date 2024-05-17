package at.tuwien.service.impl;

import at.tuwien.api.database.table.columns.ColumnDto;
import at.tuwien.api.database.table.internal.PrivilegedTableDto;
import at.tuwien.mapper.DataMapper;
import at.tuwien.mapper.MetadataMapper;
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
public class QueueServiceRabbitMqImpl extends HibernateConnector implements QueueService {

    private final DataMapper dataMapper;
    private final MetadataMapper metadataMapper;

    @Autowired
    public QueueServiceRabbitMqImpl(DataMapper dataMapper, MetadataMapper metadataMapper) {
        this.dataMapper = dataMapper;
        this.metadataMapper = metadataMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public void insert(PrivilegedTableDto table, Map<String, Object> data) throws SQLException {
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(table.getDatabase());
        final Connection connection = dataSource.getConnection();
        try {
            final int[] idx = new int[]{1};
            final PreparedStatement preparedStatement = connection.prepareStatement(
                    dataMapper.rabbitMqTupleToInsertOrUpdateQuery(metadataMapper.privilegedTableDtoToTableDto(table), data));
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                final Optional<ColumnDto> optional = table.getColumns().stream().filter(c -> c.getInternalName().equals(entry.getKey())).findFirst();
                if (optional.isEmpty()) {
                    log.error("Failed to find column with name {} in table with name {}, available columns are {}", entry.getKey(), table.getInternalName(), table.getColumns().stream().map(ColumnDto::getInternalName).toList());
                    continue;
                }
                dataMapper.prepareStatementWithColumnTypeObject(preparedStatement, optional.get().getColumnType(), idx[0]++,
                        entry.getValue());
            }
            log.trace("successfully inserted tuple");
        } finally {
            dataSource.close();
        }
    }

}
