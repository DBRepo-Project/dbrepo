package at.tuwien.entities.identifier;

public enum RelatedType {

    DOI("DOI"),

    URL("URL"),

    URN("URN"),

    ARK("ARK"),

    ARXIV("arXiv"),

    BIBCODE("bibcode"),

    EAN13("EAN13"),

    EISSN("EISSN"),

    HANDLE("Handle"),

    IGSN("IGSN"),

    ISBN("ISBN"),

    ISTC("ISTC"),

    LISSN("LISSN"),

    LSID("LSID"),

    PMID("PMID"),

    PURL("PURL"),

    UPC("UPC"),

    W3ID("w3id");

    private String name;

    RelatedType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
