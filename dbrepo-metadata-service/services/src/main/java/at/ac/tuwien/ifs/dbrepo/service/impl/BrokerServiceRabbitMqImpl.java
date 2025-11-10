package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.config.RabbitConfig;
import at.ac.tuwien.ifs.dbrepo.core.api.amqp.GrantExchangePermissionsDto;
import at.ac.tuwien.ifs.dbrepo.core.api.amqp.GrantVirtualHostPermissionsDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.AccessType;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.DatabaseAccess;
import at.ac.tuwien.ifs.dbrepo.core.exception.BrokerServiceConnectionException;
import at.ac.tuwien.ifs.dbrepo.core.exception.BrokerServiceException;
import at.ac.tuwien.ifs.dbrepo.gateway.BrokerServiceGateway;
import at.ac.tuwien.ifs.dbrepo.service.BrokerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BrokerServiceRabbitMqImpl implements BrokerService {

    private final RabbitConfig rabbitConfig;
    private final BrokerServiceGateway brokerServiceGateway;

    public BrokerServiceRabbitMqImpl(RabbitConfig rabbitConfig, BrokerServiceGateway brokerServiceGateway) {
        this.rabbitConfig = rabbitConfig;
        this.brokerServiceGateway = brokerServiceGateway;
    }

    @Override
    public void setVirtualHostPermissions(String username) throws BrokerServiceException, BrokerServiceConnectionException {
        final GrantVirtualHostPermissionsDto permissions = GrantVirtualHostPermissionsDto.builder()
                .configure("")
                .write(".*")
                .read(".*")
                .build();
        brokerServiceGateway.grantVirtualHostPermission(username, permissions);
        log.debug("set virtual host permissions: {}", permissions);
    }

    @Override
    @Transactional(readOnly = true)
    public void setTopicExchangePermissions(String username, List<DatabaseAccess> accesses) throws BrokerServiceException, BrokerServiceConnectionException {
        final GrantExchangePermissionsDto permissions = GrantExchangePermissionsDto.builder()
                .exchange(rabbitConfig.getExchangeName())
                .write(userToExchangeWritePermissionString(username, accesses))
                .read(userToExchangeReadPermissionString(username, accesses))
                .build();
        log.debug("user with username {} has exchange permissions {}", username, permissions);
        brokerServiceGateway.grantExchangePermission(username, permissions);
        log.info("Granted user with username {} topic permissions at broker service", username);
    }

    @Transactional(readOnly = true)
    public String userToExchangeWritePermissionString(String username, List<DatabaseAccess> accesses) {
        final String permissions;
        if (accesses.isEmpty() || accesses.stream().noneMatch(a -> a.getType().equals(AccessType.WRITE_OWN) || a.getType().equals(AccessType.WRITE_ALL))) {
            permissions = "";
        } else {
            log.trace("mapping {} write permissions", accesses.size());
            permissions = "^(" + accesses.stream()
                    .map(a -> switch (a.getType()) {
                        case WRITE_OWN -> a.getDatabase()
                                .getTables()
                                .stream()
                                .filter(t -> t.getOwnedBy().equals(username))
                                .map(t -> rabbitConfig.getExchangeName() + "\\." + t.getTdbid() + "\\." + t.getId())
                                .collect(Collectors.joining("|"));
                        case WRITE_ALL -> rabbitConfig.getExchangeName() + "\\." + a.getDatabase().getId() + "\\..*";
                        default -> null;
                    })
                    .collect(Collectors.joining("|")) + ")$";
        }
        log.trace("mapped databases {} to write permissions '{}'", accesses.stream().map(a -> a.getDatabase().getInternalName()).toList(), permissions);
        return permissions;
    }

    @Transactional(readOnly = true)
    public String userToExchangeReadPermissionString(String username, List<DatabaseAccess> accesses) {
        final String permissions;
        if (accesses.isEmpty()) {
            permissions = "";
        } else {
            log.trace("mapping {} read permissions", accesses.size());
            permissions = "^(" + accesses.stream()
                    .map(a -> rabbitConfig.getExchangeName() + "\\." + a.getDatabase().getId() + "\\..*")
                    .collect(Collectors.joining("|")) + ")$";
        }
        log.trace("mapped databases {} to read permissions '{}'", accesses.stream().map(a -> a.getDatabase().getInternalName()).toList(), permissions);
        return permissions;
    }

}
