package at.tuwien.service;

import at.tuwien.entities.database.License;
import at.tuwien.exception.LicenseNotFoundException;

import java.util.List;

public interface LicenseService {

    /**
     * Finds all licenses in the metadata database.
     *
     * @return List of licenses
     */
    List<License> findAll();

    /**
     * Finds a specific license by identifier.
     *
     * @param identifier The identifier.
     * @return The license, if successful.
     * @throws LicenseNotFoundException The license was not found in the metadata database.
     */
    License find(String identifier) throws LicenseNotFoundException;
}
