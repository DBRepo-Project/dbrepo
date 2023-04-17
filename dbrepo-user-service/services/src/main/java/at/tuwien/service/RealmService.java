package at.tuwien.service;

import at.tuwien.entities.user.Realm;
import at.tuwien.exception.RealmNotFoundException;

public interface RealmService {

    /**
     * Finds a realm by name.
     *
     * @param name The realm name.
     * @return The realm, if successful.
     * @throws RealmNotFoundException The realm could not be found.
     */
    Realm find(String name) throws RealmNotFoundException;
}
