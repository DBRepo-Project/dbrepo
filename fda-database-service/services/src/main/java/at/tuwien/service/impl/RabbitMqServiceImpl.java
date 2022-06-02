package at.tuwien.service.impl;

import at.tuwien.api.amqp.CreateVirtualHostDto;
import at.tuwien.api.amqp.GrantComponentDto;
import at.tuwien.config.AmqpConfig;
import at.tuwien.entities.database.Database;
import at.tuwien.exception.AmqpException;
import at.tuwien.exception.BrokerVirtualHostCreationException;
import at.tuwien.gateway.BrokerServiceGateway;
import at.tuwien.mapper.DatabaseMapper;
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
    private final DatabaseMapper databaseMapper;
    private final DatabaseRepository databaseRepository;
    private final BrokerServiceGateway brokerServiceGateway;

    @Autowired
    public RabbitMqServiceImpl(Channel channel, AmqpConfig amqpConfig, DatabaseMapper databaseMapper,
                               DatabaseRepository databaseRepository, BrokerServiceGateway brokerServiceGateway) {
        this.channel = channel;
        this.amqpConfig = amqpConfig;
        this.databaseMapper = databaseMapper;
        this.databaseRepository = databaseRepository;
        this.brokerServiceGateway = brokerServiceGateway;
    }

    @Override
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
        final GrantComponentDto grantDto = GrantComponentDto.builder()
                .name(database.getInternalName())
                .username(principal.getName())
                .build();
        try {
            channel.exchangeDeclare(database.getExchange(), BuiltinExchangeType.FANOUT, true);
            log.info("Declared exchange {}", database.getExchange());
            log.debug("grant permission {}", grantDto);
            brokerServiceGateway.grantPermission(grantDto);
        } catch (IOException e) {
            log.error("Failed to declare exchange {}", database.getExchange());
            throw new AmqpException("Failed to declare exchange", e);
        } catch (BrokerVirtualHostCreationException e) {
            log.error("Failed to grant permissions {}", database.getInternalName());
            throw new AmqpException("Failed to grant permissions", e);
        }
    }

    @Override
    public void createVirtualHost(Database database, Principal principal) throws BrokerVirtualHostCreationException {
        final CreateVirtualHostDto createDto = databaseMapper.databaseToCreateVirtualHostDto(database);
        final GrantComponentDto grantDto = GrantComponentDto.builder()
                .name(createDto.getName())
                .username(principal.getName())
                .build();
        log.debug("create virtual host {}", createDto);
        brokerServiceGateway.createVirtualHost(createDto);
        log.debug("grant permission {}", grantDto);
        brokerServiceGateway.grantPermission(grantDto);
        log.info("Created virtual host {} and granted all permissions for username {}", createDto.getName(),
                principal.getName());
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
