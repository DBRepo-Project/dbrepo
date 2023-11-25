package at.tuwien.entities.identifier;

import lombok.Getter;

@Getter
public enum IdentifierType {

    DATABASE("database"),

    SUBSET("subset"),

    VIEW("view");

    private String name;

    IdentifierType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
