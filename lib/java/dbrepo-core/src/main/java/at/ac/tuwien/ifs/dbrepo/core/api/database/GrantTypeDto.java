package at.ac.tuwien.ifs.dbrepo.core.api.database;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema
public enum GrantTypeDto {

    @JsonProperty("read")
    READ("read"),

    @JsonProperty("write")
    WRITE("write");

    private final String name;

    GrantTypeDto(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
