package at.ac.tuwien.ifs.dbrepo.core.api.keycloak;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public enum CredentialTypeDto {

    @JsonProperty("password")
    PASSWORD("password");

    private final String name;

    CredentialTypeDto(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
