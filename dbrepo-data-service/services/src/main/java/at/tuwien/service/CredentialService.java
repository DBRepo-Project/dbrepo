package at.tuwien.service;

import at.ac.tuwien.ifs.dbrepo.core.api.keycloak.TokenDto;

public interface CredentialService {

    /**
     * Gets credentials for a user with given id in a database with given id either from the cache (if not expired) or
     * retrieves them from the Metadata Service.
     *
     * @param username The username.
     * @param password The user password.
     * @return The credentials.
     */
    TokenDto getAccessToken(String username, String password);
}
