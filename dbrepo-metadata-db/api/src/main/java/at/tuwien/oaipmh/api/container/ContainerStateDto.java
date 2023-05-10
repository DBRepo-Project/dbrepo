package at.tuwien.api.container;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;

@Getter
public enum ContainerStateDto {

    @JsonProperty("created")
    CREATED("created"),

    @JsonProperty("restarting")
    RESTARTING("restarting"),

    @JsonProperty("running")
    RUNNING("running"),

    @JsonProperty("paused")
    PAUSED("paused"),

    @JsonProperty("exited")
    EXITED("exited"),

    @JsonProperty("dead")
    DEAD("dead");

    private String name;

    ContainerStateDto(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
