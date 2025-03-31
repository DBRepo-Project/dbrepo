package at.ac.tuwien.ifs.dbrepo.core.api.database.query;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public enum DatasourceType {

    @JsonProperty("table")
    TABLE("table"),

    @JsonProperty("view")
    VIEW("view");

    private final String name;

    DatasourceType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
