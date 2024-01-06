package at.tuwien.entities.identifier;

import lombok.Getter;

@Getter
public enum IdentifierType {
    DATABASE,
    SUBSET,
    TABLE,
    VIEW;
}
