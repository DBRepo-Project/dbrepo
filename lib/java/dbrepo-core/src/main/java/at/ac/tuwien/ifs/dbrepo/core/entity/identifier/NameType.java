package at.ac.tuwien.ifs.dbrepo.core.entity.identifier;

import lombok.Getter;

@Getter
public enum NameType {
    PERSONAL("Personal"),
    ORGANIZATIONAL("Organizational");

    private final String dataCiteValue;

    NameType(String dataCiteValue) {
        this.dataCiteValue = dataCiteValue;
    }
}
