package at.tuwien.api.database.table;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema
public enum HistoryEventTypeDto {

    @JsonProperty("read")
    READ("read"),

    @JsonProperty("write_own")
    WRITE_OWN("write_own"),

    @JsonProperty("write_all")
    WRITE_ALL("write_all");

    private String name;

    HistoryEventTypeDto(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
