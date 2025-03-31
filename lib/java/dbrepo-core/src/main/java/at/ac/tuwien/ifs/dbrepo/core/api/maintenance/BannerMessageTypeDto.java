package at.ac.tuwien.ifs.dbrepo.core.api.maintenance;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public enum BannerMessageTypeDto {

    @JsonProperty("error")
    ERROR("error"),

    @JsonProperty("warning")
    WARNING("warning"),

    @JsonProperty("info")
    INFO("info");

    private final String name;

    BannerMessageTypeDto(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
