package at.tuwien.entities.maintenance;

import lombok.Getter;

@Getter
public enum BannerMessageType {

    WARNING("warning"),

    ERROR("error"),

    INFO("info");

    private String name;

    BannerMessageType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}