package at.tuwien.api.database;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public enum AccessTypeDto {

    @JsonProperty("read")
    READ("read"),

    @JsonProperty("write_own")
    WRITE_OWN("write_own"),

    @JsonProperty("write_all")
    WRITE_ALL("write_all");

    private String name;

    AccessTypeDto(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
