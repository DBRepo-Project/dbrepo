package at.ac.tuwien.ifs.dbrepo.listener;

import at.ac.tuwien.ifs.dbrepo.core.api.replication.TupleReplicationTimestampDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.observation.annotation.Observed;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ReplicationTimestampListener implements MessageListener {

    private final ObjectMapper objectMapper;

    @Autowired
    public ReplicationTimestampListener(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
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
        } catch (Exception e) {
            log.error("replication-timestamps: failed to parse message: {}", e.getMessage());
        }
    }
}


