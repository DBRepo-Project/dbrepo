package at.tuwien.service;

import at.tuwien.api.keycloak.TokenDto;

public interface CredentialService {
    TokenDto getAccessToken(String username, String password);
}
