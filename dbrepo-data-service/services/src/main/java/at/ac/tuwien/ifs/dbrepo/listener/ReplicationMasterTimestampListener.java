package at.ac.tuwien.ifs.dbrepo.listener;

import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.TupleReplicationTimestampDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.DatabaseNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.exception.MetadataServiceException;
import at.ac.tuwien.ifs.dbrepo.core.exception.RemoteUnavailableException;
import at.ac.tuwien.ifs.dbrepo.core.exception.TableNotFoundException;
import at.ac.tuwien.ifs.dbrepo.service.CacheService;
import at.ac.tuwien.ifs.dbrepo.service.TableService;
import at.ac.tuwien.ifs.dbrepo.service.ReplicationForwardingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.observation.annotation.Observed;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.List;

@Slf4j
@Component
public class ReplicationMasterTimestampListener implements MessageListener {

    private final ObjectMapper objectMapper;
    private final TableService tableService;
    private final CacheService cacheService;
    private final ReplicationForwardingService replicationForwardingService;

    @Autowired
    public ReplicationMasterTimestampListener(ObjectMapper objectMapper, TableService tableService, CacheService cacheService, ReplicationForwardingService replicationForwardingService) {
        this.objectMapper = objectMapper;
        this.tableService = tableService;
        this.cacheService = cacheService;
        this.replicationForwardingService = replicationForwardingService;
    }

    @Override
    @Observed(name = "dbrepo_replication_timestamp_receive")
    @Operation(summary = "Received replication timestamp message")
    public void onMessage(Message message) {
        final MessageProperties properties = message.getMessageProperties();
        try {
            final TupleReplicationTimestampDto dto = objectMapper.readValue(message.getBody(), TupleReplicationTimestampDto.class);
            log.info("replication-timestamps: received message routingKey={}, replicationId={}, siteUrl={}, dbId={}, tableId={}, rowStart={}, rowEnd={}",
                    properties.getReceivedRoutingKey(),
                    dto.getReplicationId(),
                    dto.getSiteUrl(),
                    dto.getDatabaseId(),
                    dto.getTableId(),
                    dto.getRowStart(),
                    dto.getRowEnd());
            
            // Get database and table from cache service (which handles remote->local mapping)
            final DatabaseDto database = cacheService.getLocalDatabaseByRemoteDatabaseId(dto.getDatabaseId());
            final TableDto table = cacheService.getLocalTableByRemoteTableId(database.getId(), dto.getTableId());
            
            log.info("Retrieved database={} and table={} from metadata service", 
                    database.getInternalName(), table.getInternalName());
            
            // Process and persist timestamps using the service (same as TableEndpoint)
            List<TupleReplicationTimestampDto> timestamps = List.of(dto);
            tableService.processReplicationTimestamps(database, table, timestamps);
            
            log.info("Successfully processed replication timestamp for databaseId={}, tableId={}, replicationId={}", 
                    dto.getDatabaseId(), dto.getTableId(), dto.getReplicationId());
            
            // Forward timestamp to other replicas via forwarding queue using shared service
            final String sourceSiteId = replicationForwardingService.extractSourceSiteId(dto.getSiteUrl());
            if (sourceSiteId == null) {
                log.warn("Could not extract source site ID from siteUrl: {}, skipping forwarding", dto.getSiteUrl());
            } else {
                replicationForwardingService.forwardTimestampToForwardingQueue(dto, database, sourceSiteId);
            }
            
        } catch (DatabaseNotFoundException e) {
            log.error("Database not found for replication timestamp databaseId={}: {}", 
                    properties.getReceivedRoutingKey(), e.getMessage());
        } catch (TableNotFoundException e) {
            log.error("Table not found for replication timestamp routingKey={}: {}", 
                    properties.getReceivedRoutingKey(), e.getMessage());
        } catch (RemoteUnavailableException e) {
            log.error("Metadata service unavailable for replication timestamp routingKey={}: {}", 
                    properties.getReceivedRoutingKey(), e.getMessage());
        } catch (MetadataServiceException e) {
            log.error("Metadata service error for replication timestamp routingKey={}: {}", 
                    properties.getReceivedRoutingKey(), e.getMessage());
        } catch (SQLException e) {
            log.error("SQL error processing replication timestamp routingKey={}: {}", 
                    properties.getReceivedRoutingKey(), e.getMessage());
        } catch (Exception e) {
            log.error("replication-timestamps: failed to process message routingKey={}: {}", 
                    properties.getReceivedRoutingKey(), e.getMessage());
        }
    }
}