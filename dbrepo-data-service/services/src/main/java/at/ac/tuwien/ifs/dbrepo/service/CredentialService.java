package at.ac.tuwien.ifs.dbrepo.service;

public interface CredentialService {

    String getAdminToken(String username, String password);

    /**
     * Gets credentials for a user with given id in a database with given id either from the cache (if not expired) or
     * retrieves them from the Metadata Service.
     *
     * @param username The username.
     * @param password The user password.
     * @return The token.
     */
    String getUserToken(String username, String password);
}
