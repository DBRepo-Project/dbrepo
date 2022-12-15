package at.tuwien.service.impl;

import at.tuwien.api.database.table.TableCsvDto;
import at.tuwien.exception.*;
import at.tuwien.service.MessageQueueService;
import at.tuwien.service.QueryService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import lombok.extern.log4j.Log4j2;
import org.apache.http.auth.BasicUserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

import java.io.IOException;
import java.util.HashMap;

@Log4j2
@Service
public class RabbitMqServiceImpl implements MessageQueueService {

    private final Channel channel;
    private final ObjectMapper objectMapper;
    private final QueryService queryService;

    @Autowired
    public RabbitMqServiceImpl(Channel channel, ObjectMapper objectMapper, QueryService queryService) {
        this.channel = channel;
        this.objectMapper = objectMapper;
        this.queryService = queryService;
    }

    @Override
    @Transactional(readOnly = true)
    public void createConsumer(String queueName, Long containerId, Long databaseId, Long tableId) throws AmqpException {
        try {
            final String consumerTag = channel.basicConsume(queueName, true, new Consumer() {
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
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
                    log.trace("handle delivery of tuple, consumerTag={}, envelope={}, properties={}, body=(bytes)",
                            consumerTag, envelope, properties);
                    final TypeReference<HashMap<String, Object>> payloadReference = new TypeReference<>() {
                    };
                    try {
                        final TableCsvDto data = TableCsvDto.builder()
                                .data(objectMapper.readValue(body, payloadReference))
                                .build();
                        log.trace("received tuple data {}", data);
                        queryService.insert(containerId, databaseId, tableId, data, new BasicUserPrincipal(properties.getUserId()));
                    } catch (IOException e) {
                        log.error("Failed to parse for table with id {}, reason: {}", tableId, e.getMessage());
                        /* ignore */
                    } catch (HttpClientErrorException.Unauthorized e) {
                        log.error("Failed to authenticate for table with id {}, reason: {}", tableId, e.getMessage());
                        /* ignore */
                    } catch (HttpClientErrorException.BadRequest e) {
                        log.error("Failed to insert for table with id {}, reason: {}", tableId, e.getMessage());
                        /* ignore */
                    } catch (TableNotFoundException e) {
                        log.error("Failed to find table with id {}, reason: {}", tableId, e.getMessage());
                        /* ignore */
                    } catch (TableMalformedException e) {
                        log.error("Tuple columns do not math table columns with table id {}, reason: {}", tableId,
                                e.getMessage());
                        /* ignore */
                    } catch (DatabaseNotFoundException e) {
                        log.error("Failed to find database with id {}, reason: {}", databaseId, e.getMessage());
                        /* ignore */
                    } catch (ImageNotSupportedException e) {
                        /* ignore */
                    } catch (ContainerNotFoundException e) {
                        log.error("Failed to find container with id {}, reason: {}", containerId, e.getMessage());
                        /* ignore */
                    } catch (DatabaseConnectionException e) {
                        log.error("Failed to connect to container with id {}, reason: {}", containerId, e.getMessage());
                        /* ignore */
                    } catch (UserNotFoundException e) {
                        log.error("Failed to find user with id {}", properties.getUserId());
                        /* ignore */
                    }
                }
            });
            log.debug("declared consumer for queue name {} with tag {}", queueName, consumerTag);
        } catch (IOException e) {
            log.error("Failed to create consumer for table with id {}, reason: {}", tableId, e.getMessage());
            throw new AmqpException("Failed to create consumer", e);
        } catch (Exception e) {
            log.error("Failed unknown: {}", e.getMessage());
            /* ignore */
        }
    }

}
