package at.tuwien.api.container;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public enum ContainerActionTypeDto {

    @JsonProperty("start")
    START("start"),

    @JsonProperty("stop")
    STOP("stop");

    private String name;

    ContainerActionTypeDto(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
