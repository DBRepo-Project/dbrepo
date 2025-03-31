package at.ac.tuwien.ifs.dbrepo.core.api.database.table;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema
public enum HistoryEventTypeDto {

    @JsonProperty("insert")
    INSERT("insert"),

    @JsonProperty("delete")
    DELETE("delete");

    private final String name;

    HistoryEventTypeDto(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
