package at.ac.tuwien.ifs.dbrepo.gateway;

import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseBriefDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.grafana.CreateDashboardDto;
import at.ac.tuwien.ifs.dbrepo.core.api.grafana.CreateDashboardResponseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.grafana.PermissionTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.DashboardServiceConnectionException;
import at.ac.tuwien.ifs.dbrepo.core.exception.DashboardServiceException;

public interface DashboardServiceGateway {

    /**
     * Updates a dashboard configuration by given database.
     * @param database The database.
     * @throws DashboardServiceConnectionException The connection to the dashboard service failed.
     * @throws DashboardServiceException The dashboard service failed to update the dashboard configuration.
     */
    void update(DatabaseDto database) throws DashboardServiceConnectionException, DashboardServiceException;

    /**
     * Creates a dashboard by given metadata.
     * @param data The metadata.
     * @return The dashboard response.
     * @throws DashboardServiceConnectionException The connection to the dashboard service failed.
     * @throws DashboardServiceException The dashboard service failed to create the dashboard.
     */
    CreateDashboardResponseDto create(CreateDashboardDto data) throws DashboardServiceConnectionException,
            DashboardServiceException;

    /**
     * Updates the access on a dashboard for a given user by given dashboard uid and username.
     * @param dashboardUid The dashboard uid.
     * @param username The username.
     * @param permission The access.
     * @throws DashboardServiceConnectionException The connection to the dashboard service failed.
     * @throws DashboardServiceException The dashboard service failed to update access to the dashboard.
     */
    void updateAccess(String dashboardUid, String username, PermissionTypeDto permission)
            throws DashboardServiceConnectionException, DashboardServiceException;

    /**
     * Updates the access on a dashboard for anonymous users by given dashboard uid.
     * @param dashboardUid The dashboard uid.
     * @param database The database.
     * @throws DashboardServiceConnectionException The connection to the dashboard service failed.
     * @throws DashboardServiceException The dashboard service failed to update access to the dashboard.
     */
    void updateAnonymousAccess(String dashboardUid, DatabaseBriefDto database)
            throws DashboardServiceConnectionException, DashboardServiceException;
}
