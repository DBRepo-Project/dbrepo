package at.ac.tuwien.ifs.dbrepo.core.entity.cache;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public enum SubsetType {

    @JsonProperty("query")
    QUERY("query"),

    @JsonProperty("view")
    VIEW("view");

    private final String name;

    SubsetType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
