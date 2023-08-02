package at.tuwien.api.identifier;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public enum RelatedTypeDto {

    @JsonProperty("DOI")
    DOI("DOI"),

    @JsonProperty("URL")
    URL("URL"),

    @JsonProperty("URN")
    URN("URN"),

    @JsonProperty("ARK")
    ARK("ARK"),

    @JsonProperty("arXiv")
    ARXIV("arXiv"),

    @JsonProperty("bibcode")
    BIBCODE("bibcode"),

    @JsonProperty("EAN13")
    EAN13("EAN13"),

    @JsonProperty("EISSN")
    EISSN("EISSN"),

    @JsonProperty("Handle")
    HANDLE("Handle"),

    @JsonProperty("IGSN")
    IGSN("IGSN"),

    @JsonProperty("ISBN")
    ISBN("ISBN"),

    @JsonProperty("ISTC")
    ISTC("ISTC"),

    @JsonProperty("LISSN")
    LISSN("LISSN"),

    @JsonProperty("LSID")
    LSID("LSID"),

    @JsonProperty("PMID")
    PMID("PMID"),

    @JsonProperty("PURL")
    PURL("PURL"),

    @JsonProperty("UPC")
    UPC("UPC"),

    @JsonProperty("w3id")
    W3ID("w3id");

    private String name;

    RelatedTypeDto(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
