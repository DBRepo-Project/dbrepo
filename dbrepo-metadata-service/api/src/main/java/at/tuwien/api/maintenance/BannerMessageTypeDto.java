package at.tuwien.api.maintenance;

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

    private String name;

    BannerMessageTypeDto(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
