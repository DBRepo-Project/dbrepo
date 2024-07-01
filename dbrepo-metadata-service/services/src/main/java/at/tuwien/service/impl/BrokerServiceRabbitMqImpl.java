package at.tuwien.service.impl;

import at.tuwien.api.amqp.GrantExchangePermissionsDto;
import at.tuwien.api.amqp.GrantVirtualHostPermissionsDto;
import at.tuwien.config.RabbitConfig;
import at.tuwien.entities.database.AccessType;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.gateway.BrokerServiceGateway;
import at.tuwien.service.BrokerService;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Log4j2
@Service
public class BrokerServiceRabbitMqImpl implements BrokerService {

    private final RabbitConfig rabbitConfig;
    private final BrokerServiceGateway brokerServiceGateway;

    public BrokerServiceRabbitMqImpl(RabbitConfig rabbitConfig, BrokerServiceGateway brokerServiceGateway) {
        this.rabbitConfig = rabbitConfig;
        this.brokerServiceGateway = brokerServiceGateway;
    }

    @Override
    public void setVirtualHostPermissions(User user) throws ServiceException, ServiceConnectionException {
        final GrantVirtualHostPermissionsDto permissions = GrantVirtualHostPermissionsDto.builder()
                .configure("")
                .write(".*")
                .read(".*")
                .build();
        brokerServiceGateway.grantVirtualHostPermission(user.getUsername(), permissions);
        log.info("Set virtual host permissions");
    }

    @Override
    @Transactional(readOnly = true)
    public void setTopicExchangePermissions(User user) throws ServiceException, ServiceConnectionException {
        final GrantExchangePermissionsDto permissions = GrantExchangePermissionsDto.builder()
                .exchange(rabbitConfig.getExchangeName())
                .write(userToExchangeWritePermissionString(user))
                .read(userToExchangeReadPermissionString(user))
                .build();
        log.debug("user with username {} has exchange permissions {}", user.getUsername(), permissions);
        brokerServiceGateway.grantExchangePermission(user.getUsername(), permissions);
        log.info("Granted user with username {} topic permissions at broker service", user.getUsername());
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
                                .map(t -> rabbitConfig.getExchangeName() + "\\." + t.getTdbid() + "\\." + t.getId())
                                .collect(Collectors.joining("|"));
                        case WRITE_ALL -> rabbitConfig.getExchangeName() + "\\." + a.getDatabase().getId() + "\\..*";
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
                    .map(a -> rabbitConfig.getExchangeName() + "\\." + a.getDatabase().getId() + "\\..*")
                    .collect(Collectors.joining("|")) + ")$";
        }
        log.trace("mapped databases {} to read permissions '{}'", user.getAccesses().stream().map(a -> a.getDatabase().getInternalName()).toList(), permissions);
        return permissions;
    }

}
