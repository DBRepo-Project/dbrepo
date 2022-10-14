package at.tuwien.api.container.image;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum ImageEnvItemTypeDto {

    @JsonProperty("username")
    USERNAME("username"),

    @JsonProperty("password")
    PASSWORD("password"),

    @JsonProperty("privileged_username")
    PRIVILEGED_USERNAME("privileged_username"),

    @JsonProperty("privileged_password")
    PRIVILEGED_PASSWORD("privileged_password");

    private String name;

    ImageEnvItemTypeDto(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
