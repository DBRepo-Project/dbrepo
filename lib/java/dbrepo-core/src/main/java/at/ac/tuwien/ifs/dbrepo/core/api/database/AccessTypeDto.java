package at.ac.tuwien.ifs.dbrepo.core.api.database;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema
public enum AccessTypeDto {

    @JsonProperty("read")
    READ("read"),

    @JsonProperty("write_own")
    WRITE_OWN("write_own"),

    @JsonProperty("write_all")
    WRITE_ALL("write_all");

    private final String name;

    AccessTypeDto(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
