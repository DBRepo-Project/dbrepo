package at.ac.tuwien.ifs.dbrepo.core.entity.identifier;

public enum IdentifierFunderType {
    CROSSREF_FUNDER_ID,
    ROR,
    GND,
    ISNI,
    OTHER;

    public String getDataCiteValue() {
        return switch (this) {
            case CROSSREF_FUNDER_ID -> "Crossref Funder ID";
            case ROR, ISNI -> name();
            // Kernel 4.4 has no separate value for a GND identifier.
            case GND, OTHER -> "Other";
        };
    }
}
