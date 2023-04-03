package at.tuwien.gateway;

import at.tuwien.api.auth.CreateUserDto;
import at.tuwien.api.auth.TokenDto;
import at.tuwien.exception.RemoteUnavailableException;

public interface GatewayServiceGateway {
    TokenDto getToken() throws RemoteUnavailableException;

    void createUser(String token, CreateUserDto data) throws RemoteUnavailableException;
}
