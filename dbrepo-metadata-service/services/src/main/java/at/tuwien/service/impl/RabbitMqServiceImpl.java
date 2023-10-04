package at.tuwien.service.impl;

import at.tuwien.api.amqp.GrantVirtualHostPermissionsDto;
import at.tuwien.entities.user.User;
import at.tuwien.exception.BrokerRemoteException;
import at.tuwien.exception.BrokerVirtualHostModificationException;
import at.tuwien.exception.BrokerVirtualHostGrantException;
import at.tuwien.gateway.BrokerServiceGateway;
import at.tuwien.mapper.AmqpMapper;
import at.tuwien.repository.mdb.DatabaseRepository;
import at.tuwien.service.MessageQueueService;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class RabbitMqServiceImpl implements MessageQueueService {

    private final AmqpMapper amqpMapper;
    private final DatabaseRepository databaseRepository;
    private final BrokerServiceGateway brokerServiceGateway;

    public RabbitMqServiceImpl(AmqpMapper amqpMapper, DatabaseRepository databaseRepository,
                               BrokerServiceGateway brokerServiceGateway) {
        this.amqpMapper = amqpMapper;
        this.databaseRepository = databaseRepository;
        this.brokerServiceGateway = brokerServiceGateway;
    }

    @Override
    public void createUser(String username) throws BrokerRemoteException, BrokerVirtualHostModificationException {
        brokerServiceGateway.createUser(username);
    }

    @Override
    public void deleteUser(String username) throws BrokerRemoteException, BrokerVirtualHostModificationException {
        brokerServiceGateway.deleteUser(username);
    }

    @Override
    public void updatePermissions(User user) throws BrokerRemoteException, BrokerVirtualHostGrantException {
        final GrantVirtualHostPermissionsDto permissions = GrantVirtualHostPermissionsDto.builder()
                .configure(amqpMapper.databaseListToPermissionString(databaseRepository.findConfigureAccess(user.getId())))
                .write(amqpMapper.databaseListToPermissionString(databaseRepository.findWriteAccess(user.getId())))
                .read(amqpMapper.databaseListToPermissionString(databaseRepository.findReadAccess(user.getId())))
                .build();
        log.trace("mapped permissions {}", permissions);
        brokerServiceGateway.grantPermission(user.getUsername(), permissions);
    }

}
