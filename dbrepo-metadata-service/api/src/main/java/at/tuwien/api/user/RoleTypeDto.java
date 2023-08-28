package at.tuwien.api.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public enum RoleTypeDto {

    @JsonProperty("researcher")
    ROLE_RESEARCHER("researcher"),

    @JsonProperty("developer")
    ROLE_DEVELOPER("developer"),

    @JsonProperty("data_steward")
    ROLE_DATA_STEWARD("data_steward");

    private String name;

    RoleTypeDto(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
