package at.ac.tuwien.ifs.dbrepo.core.entity.identifier;

import lombok.Getter;

@Getter
public enum IdentifierType {
    DATABASE,
    SUBSET,
    TABLE,
    VIEW
}
