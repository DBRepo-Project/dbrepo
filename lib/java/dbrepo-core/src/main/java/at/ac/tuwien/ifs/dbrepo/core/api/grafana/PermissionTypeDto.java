package at.ac.tuwien.ifs.dbrepo.core.api.grafana;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema
public enum PermissionTypeDto {

    @JsonProperty("View")
    VIEW("View"),

    @JsonProperty("Editor")
    EDITOR("Editor"),

    @JsonProperty("Admin")
    ADMIN("Admin"),

    @JsonProperty("")
    NONE("");

    private final String name;

    PermissionTypeDto(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
