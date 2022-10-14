package at.tuwien.service.impl;

import at.tuwien.api.amqp.GrantVirtualHostPermissionsDto;
import at.tuwien.config.AmqpConfig;
import at.tuwien.entities.database.Database;
import at.tuwien.exception.AmqpException;
import at.tuwien.exception.BrokerVirtualHostCreationException;
import at.tuwien.gateway.BrokerServiceGateway;
import at.tuwien.mapper.AmqpMapper;
import at.tuwien.repository.jpa.DatabaseRepository;
import at.tuwien.service.MessageQueueService;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import lombok.extern.log4j.Log4j2;
import org.apache.http.auth.BasicUserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
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
    private final BrokerServiceGateway brokerServiceGateway;

    @Autowired
    public RabbitMqServiceImpl(Channel channel, AmqpConfig amqpConfig, AmqpMapper amqpMapper,
                               DatabaseRepository databaseRepository, BrokerServiceGateway brokerServiceGateway) {
        this.channel = channel;
        this.amqpConfig = amqpConfig;
        this.amqpMapper = amqpMapper;
        this.databaseRepository = databaseRepository;
        this.brokerServiceGateway = brokerServiceGateway;
    }

    @PostConstruct
    public void init() throws AmqpException {
        final List<Database> databases = databaseRepository.findAll();
        final Principal principal = new BasicUserPrincipal(amqpConfig.getAmpqUsername());
        for (Database database : databases) {
            createExchange(database, principal);
        }
    }

    @Override
    public void createExchange(Database database, Principal principal) throws AmqpException {
        try {
            channel.exchangeDeclare(database.getExchange(), BuiltinExchangeType.FANOUT, true);
            log.info("Declared exchange {}", database.getExchange());
        } catch (IOException e) {
            log.error("Failed to declare exchange {}", database.getExchange());
            throw new AmqpException("Failed to declare exchange", e);
        }
    }

    @Override
    public void updatePermissions(Principal principal) throws BrokerVirtualHostCreationException {
        final List<Database> databases = databaseRepository.findAllByUsername(principal.getName());
        final GrantVirtualHostPermissionsDto permissions = amqpMapper.databasesToGrantVirtualHostPermissionsDto(databases);
        brokerServiceGateway.grantPermission(principal.getName(), permissions);
    }

    @Override
    public void deleteExchange(Database database) throws AmqpException {
        try {
            channel.exchangeDelete(database.getExchange());
            log.info("Deleted exchange {}", database.getExchange());
        } catch (IOException e) {
            log.error("Failed to delete exchange {}", database.getExchange());
            throw new AmqpException("Failed to delete exchange", e);
        }
    }

}
