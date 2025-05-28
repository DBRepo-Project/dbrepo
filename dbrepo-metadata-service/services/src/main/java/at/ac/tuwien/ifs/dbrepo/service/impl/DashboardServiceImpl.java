package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.database.AccessTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.api.grafana.CreateDashboardResponseDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.user.User;
import at.ac.tuwien.ifs.dbrepo.core.exception.DashboardServiceConnectionException;
import at.ac.tuwien.ifs.dbrepo.core.exception.DashboardServiceException;
import at.ac.tuwien.ifs.dbrepo.core.mapper.MetadataMapper;
import at.ac.tuwien.ifs.dbrepo.gateway.DashboardServiceGateway;
import at.ac.tuwien.ifs.dbrepo.service.DashboardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DashboardServiceImpl implements DashboardService {

    private final MetadataMapper metadataMapper;
    private final DashboardServiceGateway dashboardServiceGateway;

    @Autowired
    public DashboardServiceImpl(MetadataMapper metadataMapper, DashboardServiceGateway dashboardServiceGateway) {
        this.metadataMapper = metadataMapper;
        this.dashboardServiceGateway = dashboardServiceGateway;
    }

    @Override
    public void update(Database database) throws DashboardServiceException, DashboardServiceConnectionException {
        if (!database.getIsDashboardEnabled()) {
            log.trace("database does not manage their dashboard, skip");
            return;
        }
        dashboardServiceGateway.update(metadataMapper.databaseToDatabaseDto(database));
    }

    @Override
    public CreateDashboardResponseDto create(Database database) throws DashboardServiceException,
            DashboardServiceConnectionException {
        return dashboardServiceGateway.create(metadataMapper.databaseToCreateDashboardDto(database));
    }

    @Override
    public void updateAccess(Database database, User user, AccessTypeDto access) throws DashboardServiceException,
            DashboardServiceConnectionException {
        dashboardServiceGateway.updateAccess(database.getDashboardUid(), user.getUsername(),
                metadataMapper.accessTypeDtoToPermissionTypeDto(access));
    }

}
