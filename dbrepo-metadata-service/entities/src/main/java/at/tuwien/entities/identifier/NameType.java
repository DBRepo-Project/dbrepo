package at.tuwien.entities.identifier;

import lombok.Getter;

@Getter
public enum NameType {

    PERSONAL("Personal"),

    ORGANIZATIONAL("Organizational");

    private String name;

    NameType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
