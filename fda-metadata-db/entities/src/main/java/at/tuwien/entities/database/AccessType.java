package at.tuwien.entities.database;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public enum AccessType {
    READ,
    WRITE_OWN,
    WRITE_ALL;
}
