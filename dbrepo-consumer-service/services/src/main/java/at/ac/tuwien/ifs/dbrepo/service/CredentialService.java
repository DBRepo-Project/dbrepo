package at.ac.tuwien.ifs.dbrepo.service;

public interface CredentialService {

    /**
     * Reactive implementation of a cache updater for credentials.
     *
     * @param username The user name.
     * @param password The user password.
     * @return The auth token.
     */
    String getAccessToken(String username, String password);
}
