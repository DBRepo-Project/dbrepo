
package at.tuwien.entities.identifier;

import lombok.Getter;

@Getter
public enum NameIdentifierSchemeType {

    ORCID("orcid"),

    ROR("ror"),

    ISNI("isni"),

    GRID("grid");

    private String name;

    NameIdentifierSchemeType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
