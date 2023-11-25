package at.tuwien.entities.database;

import lombok.Getter;
import lombok.ToString;

@Getter
public enum AccessType {

    READ("read"),

    WRITE_OWN("write_own"),

    WRITE_ALL("write_all");

    private String name;

    AccessType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
