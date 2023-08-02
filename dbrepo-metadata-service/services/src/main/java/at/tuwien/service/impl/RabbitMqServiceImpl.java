package at.tuwien.service.impl;

import at.tuwien.api.amqp.GrantVirtualHostPermissionsDto;
import at.tuwien.config.AmqpConfig;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.user.User;
import at.tuwien.exception.AmqpException;
import at.tuwien.exception.BrokerVirtualHostCreationException;
import at.tuwien.exception.BrokerVirtualHostGrantException;
import at.tuwien.gateway.BrokerServiceGateway;
import at.tuwien.mapper.AmqpMapper;
import at.tuwien.repository.mdb.DatabaseRepository;
import at.tuwien.repository.mdb.TableRepository;
import at.tuwien.service.MessageQueueService;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import lombok.extern.log4j.Log4j2;
import org.apache.http.auth.BasicUserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.Principal;
import java.util.List;

@Log4j2
@Service
public class RabbitMqServiceImpl implements MessageQueueService {

    private final Channel channel;
    private final AmqpConfig amqpConfig;
    private final AmqpMapper amqpMapper;
    private final DatabaseRepository databaseRepository;
    private final TableRepository tableRepository;
    private final BrokerServiceGateway brokerServiceGateway;

    @Autowired
    public RabbitMqServiceImpl(Channel channel, AmqpConfig amqpConfig, AmqpMapper amqpMapper,
                               DatabaseRepository databaseRepository, TableRepository tableRepository,
                               BrokerServiceGateway brokerServiceGateway) {
        this.channel = channel;
        this.amqpConfig = amqpConfig;
        this.amqpMapper = amqpMapper;
        this.databaseRepository = databaseRepository;
        this.tableRepository = tableRepository;
        this.brokerServiceGateway = brokerServiceGateway;
    }

    @Override
    @EventListener(ApplicationReadyEvent.class)
    @Transactional(readOnly = true)
    public void init() throws AmqpException {
        final List<Database> databases = databaseRepository.findAll();
        final Principal principal = new BasicUserPrincipal(amqpConfig.getAmpqUsername());
        for (Database database : databases) {
            createExchange(database, principal);
        }
        final List<Table> tables = tableRepository.findAll();
        for (Table table : tables) {
            create(table);
        }
    }

    @Override
    public void createExchange(Database database, Principal principal) throws AmqpException {
        try {
            channel.exchangeDeclare(database.getExchangeName(), BuiltinExchangeType.DIRECT, true);
            log.info("Declared exchange {}", database.getExchangeName());
        } catch (IOException e) {
            log.error("Failed to declare exchange {}", database.getExchangeName());
            throw new AmqpException("Failed to declare exchange", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void create(Table table) throws AmqpException {
        try {
            channel.queueDeclare(table.getQueueName(), true, false, false, null);
            channel.queueBind(table.getQueueName(), table.getDatabase().getExchangeName(), table.getRoutingKey());
        } catch (IOException e) {
            log.error("Failed to create queue and bind for table with id {}", table.getId());
            throw new AmqpException("Failed to create", e);
        }
        log.info("Created queue for table with id {}", table.getId());
    }

    @Override
    public void createUser(User user) throws BrokerVirtualHostCreationException {
        brokerServiceGateway.createUser(user.getUsername());
    }

    @Override
    public void updatePermissions(User user) throws BrokerVirtualHostGrantException {
        final GrantVirtualHostPermissionsDto permissions = GrantVirtualHostPermissionsDto.builder()
                .configure(amqpMapper.databaseListToPermissionString(databaseRepository.findConfigureAccess(user.getId())))
                .write(amqpMapper.databaseListToPermissionString(databaseRepository.findWriteAccess(user.getId())))
                .read(amqpMapper.databaseListToPermissionString(databaseRepository.findReadAccess(user.getId())))
                .build();
        log.trace("mapped permissions {}", permissions);
        brokerServiceGateway.grantPermission(user.getUsername(), permissions);
    }

    @Override
    public void deleteExchange(Database database) throws AmqpException {
        try {
            channel.exchangeDelete(database.getExchangeName());
            log.info("Deleted exchange {}", database.getExchangeName());
        } catch (IOException e) {
            log.error("Failed to delete exchange {}", database.getExchangeName());
            throw new AmqpException("Failed to delete exchange", e);
        }
    }

}
