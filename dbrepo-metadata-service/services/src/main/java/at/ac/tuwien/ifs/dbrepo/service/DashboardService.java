package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.database.AccessTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.api.grafana.CreateDashboardResponseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.UserDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.Database;
import at.ac.tuwien.ifs.dbrepo.core.exception.DashboardServiceConnectionException;
import at.ac.tuwien.ifs.dbrepo.core.exception.DashboardServiceException;

public interface DashboardService {

    /**
     * Updates the panels in the dashboard in the dashboard service for a given database. Only updates the dashboard if
     * (and only if) the {@link Database#isDashboardEnabled} is set to true.
     *
     * @param database The database.
     * @throws DashboardServiceException            The dashboard service responded with an unexpected error code.
     * @throws DashboardServiceConnectionException  The connection to the dashboard service could not be established.
     */
    void update(Database database) throws DashboardServiceException, DashboardServiceConnectionException;

    /**
     * Creates the dashboard in the dashboard service for a given database. Does not create panels.
     *
     * @param database The database.
     * @return The response containing the UID of the created dashboard.
     * @throws DashboardServiceException            The dashboard service responded with an unexpected error code.
     * @throws DashboardServiceConnectionException  The connection to the dashboard service could not be established.
     */
    CreateDashboardResponseDto create(Database database) throws DashboardServiceException,
            DashboardServiceConnectionException;

    /**
     * Updates the access on the dashboard in the dashboard service.
     *
     * @param database The database.
     * @param username The username.
     * @param access The access type.
     * @throws DashboardServiceException            The dashboard service responded with an unexpected error code.
     * @throws DashboardServiceConnectionException  The connection to the dashboard service could not be established.
     */
    void updateAccess(Database database, String username, AccessTypeDto access) throws DashboardServiceException,
            DashboardServiceConnectionException;
}
