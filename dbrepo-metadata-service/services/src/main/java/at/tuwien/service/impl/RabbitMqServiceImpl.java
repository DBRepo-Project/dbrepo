package at.tuwien.service.impl;

import at.tuwien.api.amqp.GrantExchangePermissionsDto;
import at.tuwien.api.amqp.GrantVirtualHostPermissionsDto;
import at.tuwien.entities.database.AccessType;
import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.user.User;
import at.tuwien.exception.BrokerRemoteException;
import at.tuwien.exception.BrokerVirtualHostGrantException;
import at.tuwien.exception.BrokerVirtualHostModificationException;
import at.tuwien.gateway.BrokerServiceGateway;
import at.tuwien.service.MessageQueueService;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Log4j2
@Service
public class RabbitMqServiceImpl implements MessageQueueService {

    private final BrokerServiceGateway brokerServiceGateway;

    public RabbitMqServiceImpl(BrokerServiceGateway brokerServiceGateway) {
        this.brokerServiceGateway = brokerServiceGateway;
    }

    @Override
    public void createUser(String username, String password) throws BrokerRemoteException, BrokerVirtualHostModificationException {
        brokerServiceGateway.createUser(username, password);
    }

    @Override
    public void deleteUser(String username) throws BrokerRemoteException, BrokerVirtualHostModificationException {
        brokerServiceGateway.deleteUser(username);
    }

    @Override
    public void setVirtualHostPermissions(String username) throws BrokerVirtualHostGrantException, BrokerRemoteException {
        final GrantVirtualHostPermissionsDto permissions = GrantVirtualHostPermissionsDto.builder()
                .configure("")
                .write(".*")
                .read(".*")
                .build();
        log.debug("user with username {} has virtual host permissions {}", username, permissions);
        brokerServiceGateway.grantPermission(username, permissions);
    }

    @Override
    @Transactional(readOnly = true)
    public void setTopicExchangePermissions(User user) throws BrokerVirtualHostGrantException,
            BrokerRemoteException {
        final GrantExchangePermissionsDto permissions = GrantExchangePermissionsDto.builder()
                .exchange("dbrepo")
                .write(userToExchangeWritePermissionString(user))
                .read(userToExchangeReadPermissionString(user))
                .build();
        log.debug("user with username {} has exchange permissions {}", user.getUsername(), permissions);
        brokerServiceGateway.grantTopicPermission(user.getUsername(), permissions);
    }

    @Transactional(readOnly = true)
    public String userToExchangeWritePermissionString(User user) {
        final String permissions;
        if (user.getAccesses().isEmpty() || user.getAccesses().stream().noneMatch(a -> a.getType().equals(AccessType.WRITE_OWN) || a.getType().equals(AccessType.WRITE_ALL))) {
            permissions = "";
        } else {
            log.trace("mapping {} write permissions", user.getAccesses().size());
            permissions = "^(" + user.getAccesses()
                    .stream()
                    .map(a -> switch (a.getType()) {
                        case WRITE_OWN -> a.getDatabase()
                                .getTables()
                                .stream()
                                .filter(t -> t.getOwnedBy().equals(user.getId()))
                                .map(Table::getRoutingKey)
                                .collect(Collectors.joining("|"));
                        case WRITE_ALL -> "dbrepo\\." + a.getDatabase().getInternalName() + "\\..*";
                        default -> null;
                    })
                    .collect(Collectors.joining("|")) + ")$";
        }
        log.trace("mapped databases {} to write permissions '{}'", user.getAccesses().stream().map(a -> a.getDatabase().getInternalName()).toList(), permissions);
        return permissions;
    }

    @Transactional(readOnly = true)
    public String userToExchangeReadPermissionString(User user) {
        final String permissions;
        if (user.getAccesses().isEmpty()) {
            permissions = "";
        } else {
            log.trace("mapping {} read permissions", user.getAccesses().size());
            permissions = "^(" + user.getAccesses()
                    .stream()
                    .map(a -> "dbrepo\\." + a.getDatabase().getInternalName() + "\\..*")
                    .collect(Collectors.joining("|")) + ")$";
        }
        log.trace("mapped databases {} to read permissions '{}'", user.getAccesses().stream().map(a -> a.getDatabase().getInternalName()).toList(), permissions);
        return permissions;
    }

}
