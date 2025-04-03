package at.ac.tuwien.ifs.dbrepo.core.entity.database.table.constraints.foreignKey;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public enum ReferenceType {
    RESTRICT,
    CASCADE,
    SET_NULL,
    NO_ACTION,
    SET_DEFAULT,
}
