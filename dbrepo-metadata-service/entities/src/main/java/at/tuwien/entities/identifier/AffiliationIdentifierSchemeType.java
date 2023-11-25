
package at.tuwien.entities.identifier;

import lombok.Getter;

@Getter
public enum AffiliationIdentifierSchemeType {

    ROR("ror"),

    GRID("grid"),

    ISNI("isni");

    private String name;

    AffiliationIdentifierSchemeType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
