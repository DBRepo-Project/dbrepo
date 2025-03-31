package at.ac.tuwien.ifs.dbrepo.core.api.datacite.doi;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.io.Serializable;


@Getter
public enum DataCiteDoiEvent implements Serializable {

    @JsonProperty("publish")
    PUBLISH("publish"),

    @JsonProperty("register")
    REGISTER("register"),

    @JsonProperty("hide")
    HIDE("hide");

    private final String name;

    DataCiteDoiEvent(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
