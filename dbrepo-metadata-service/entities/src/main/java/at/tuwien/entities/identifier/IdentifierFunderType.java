package at.tuwien.entities.identifier;

public enum IdentifierFunderType {

    CROSSREF_FUNDER_ID("crossref_funder_id"),

    ROR("ror"),

    GND("gnd"),

    ISNI("isni"),

    OTHER("other");

    private String name;

    IdentifierFunderType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }

}
