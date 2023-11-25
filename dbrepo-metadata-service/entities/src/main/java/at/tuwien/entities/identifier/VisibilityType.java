package at.tuwien.entities.identifier;

import lombok.Getter;
import lombok.ToString;

@Getter
public enum VisibilityType {

    EVERYONE("everyone"),

    SELF("self");

    private String name;

    VisibilityType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}