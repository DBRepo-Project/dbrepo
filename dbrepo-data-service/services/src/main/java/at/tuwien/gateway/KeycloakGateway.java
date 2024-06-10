package at.tuwien.gateway;

import at.tuwien.api.keycloak.TokenDto;
import at.tuwien.exception.RemoteUnavailableException;
import at.tuwien.exception.ServiceConnectionException;
import at.tuwien.exception.ServiceException;

import javax.naming.ServiceUnavailableException;

public interface KeycloakGateway {

    TokenDto obtainUserToken(String username, String password) throws RemoteUnavailableException, ServiceException;

}
