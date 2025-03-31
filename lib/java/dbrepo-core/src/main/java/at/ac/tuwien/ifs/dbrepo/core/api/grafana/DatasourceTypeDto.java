package at.ac.tuwien.ifs.dbrepo.core.api.grafana;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema
public enum DatasourceTypeDto {

    @JsonProperty("yesoreyeram-infinity-datasource")
    INFINITY("yesoreyeram-infinity-datasource");

    private final String name;

    DatasourceTypeDto(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
