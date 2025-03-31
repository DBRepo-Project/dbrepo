package at.ac.tuwien.ifs.dbrepo.core.api.grafana;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema
public enum AccessTypeDto {

    @JsonProperty("proxy")
    PROXY("proxy");

    private final String name;

    AccessTypeDto(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
