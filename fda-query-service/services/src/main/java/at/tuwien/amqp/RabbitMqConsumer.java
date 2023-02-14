package at.tuwien.amqp;

import at.tuwien.api.database.table.TableCsvDto;
import at.tuwien.entities.database.table.Table;
import at.tuwien.exception.*;
import at.tuwien.service.QueryService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.ShutdownSignalException;
import lombok.extern.log4j.Log4j2;
import org.apache.http.auth.BasicUserPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

import java.io.IOException;
import java.util.HashMap;

@Log4j2
public class RabbitMqConsumer implements Consumer {

    private final Long containerId;
    private final Long databaseId;
    private final Long tableId;
    private final ObjectMapper objectMapper;
    private final QueryService queryService;

    public RabbitMqConsumer(Long containerId, Long databaseId, Long tableId, ObjectMapper objectMapper,
                            QueryService queryService) {
        this.containerId = containerId;
        this.databaseId = databaseId;
        this.tableId = tableId;
        this.objectMapper = objectMapper;
        this.queryService = queryService;
    }

    @Override
    public void handleConsumeOk(String consumerTag) {
        //
    }

    @Override
    public void handleCancelOk(String consumerTag) {
        //
    }

    @Override
    public void handleCancel(String consumerTag) {
        //
    }

    @Override
    public void handleShutdownSignal(String consumerTag, ShutdownSignalException sig) {
        //
    }

    @Override
    public void handleRecoverOk(String consumerTag) {
        //
    }

    @Override
    @Transactional(readOnly = true)
    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
            throws IOException {
        log.trace("handle delivery of tuple, consumerTag={}, envelope={}, properties={}, body=(bytes)",
                consumerTag, envelope, properties);
        final TypeReference<HashMap<String, Object>> payloadReference = new TypeReference<>() {
        };
        final TableCsvDto data = TableCsvDto.builder()
                .data(objectMapper.readValue(body, payloadReference))
                .build();
        log.trace("received tuple data {}", data);
        try {
            queryService.insert(containerId, databaseId, tableId, data, new BasicUserPrincipal(properties.getUserId()));
        } catch (HttpClientErrorException.Unauthorized e) {
            log.error("Failed to authenticate for table with id {}, reason: {}", tableId, e.getMessage());
            throw new IOException("Failed to authenticate for table", e);
        } catch (HttpClientErrorException.BadRequest e) {
            log.error("Failed to insert for table with id {}, reason: {}", tableId, e.getMessage());
            throw new IOException("Failed to insert for table", e);
        } catch (TableNotFoundException e) {
            log.error("Failed to find table with id {}, reason: {}", tableId, e.getMessage());
            throw new IOException("Failed to find table", e);
        } catch (TableMalformedException e) {
            log.error("Tuple columns do not match table columns with table id {}, reason: {}", tableId, e.getMessage());
            throw new IOException("Tuple columns do not match table columns", e);
        } catch (DatabaseNotFoundException e) {
            log.error("Failed to find database with id {}, reason: {}", databaseId, e.getMessage());
            throw new IOException("Failed to find database", e);
        } catch (ImageNotSupportedException e) {
            log.error("Image is not supported");
            throw new IOException("Image is not supported", e);
        } catch (ContainerNotFoundException e) {
            log.error("Failed to find container with id {}, reason: {}", containerId, e.getMessage());
            throw new IOException("Failed to find container", e);
        } catch (DatabaseConnectionException e) {
            log.error("Failed to connect to container with id {}, reason: {}", containerId, e.getMessage());
            throw new IOException("Failed to connect to container", e);
        } catch (UserNotFoundException e) {
            log.error("Failed to find user with id {}", properties.getUserId());
            throw new IOException("Failed to find user", e);
        }
    }

}
