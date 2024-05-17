package at.tuwien.api.identifier;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public enum RelationTypeDto {

    @JsonProperty("IsCitedBy")
    IS_CITED_BY("IsCitedBy"),

    @JsonProperty("Cites")
    CITES("Cites"),

    @JsonProperty("IsSupplementTo")
    IS_SUPPLEMENT_TO("IsSupplementTo"),

    @JsonProperty("IsSupplementedBy")
    IS_SUPPLEMENTED_BY("IsSupplementedBy"),

    @JsonProperty("IsContinuedBy")
    IS_CONTINUED_BY("IsContinuedBy"),

    @JsonProperty("Continues")
    CONTINUES("Continues"),

    @JsonProperty("IsDescribedBy")
    IS_DESCRIBED_BY("IsDescribedBy"),

    @JsonProperty("Describes")
    DESCRIBES("Describes"),

    @JsonProperty("HasMetadata")
    HAS_METADATA("HasMetadata"),

    @JsonProperty("IsMetadataFor")
    IS_METADATA_FOR("IsMetadataFor"),

    @JsonProperty("HasVersion")
    HAS_VERSION("HasVersion"),

    @JsonProperty("IsVersionOf")
    IS_VERSION_OF("IsVersionOf"),

    @JsonProperty("IsNewVersionOf")
    IS_NEW_VERSION_OF("IsNewVersionOf"),

    @JsonProperty("IsPreviousVersionOf")
    IS_PREVIOUS_VERSION_OF("IsPreviousVersionOf"),

    @JsonProperty("IsPartOf")
    IS_PART_OF("IsPartOf"),

    @JsonProperty("HasPart")
    HAS_PART("HasPart"),

    @JsonProperty("IsPublishedIn")
    IS_PUBLISHED_IN("IsPublishedIn"),

    @JsonProperty("IsReferencedBy")
    IS_REFERENCED_BY("IsReferencedBy"),

    @JsonProperty("References")
    REFERENCES("References"),

    @JsonProperty("IsDocumentedBy")
    IS_DOCUMENTED_BY("IsDocumentedBy"),

    @JsonProperty("Documents")
    DOCUMENTS("Documents"),

    @JsonProperty("IsCompiledBy")
    IS_COMPILED_BY("IsCompiledBy"),

    @JsonProperty("Compiles")
    COMPILES("Compiles"),

    @JsonProperty("IsVariantFormOf")
    IS_VARIANT_FORM_OF("IsVariantFormOf"),

    @JsonProperty("IsOriginalFormOf")
    IS_ORIGINAL_FORM_OF("IsOriginalFormOf"),

    @JsonProperty("IsIdenticalTo")
    IS_IDENTICAL_TO("IsIdenticalTo"),

    @JsonProperty("IsReviewedBy")
    IS_REVIEWED_BY("IsReviewedBy"),

    @JsonProperty("Reviews")
    REVIEWS("Reviews"),

    @JsonProperty("IsDerivedFrom")
    IS_DERIVED_FROM("IsDerivedFrom"),

    @JsonProperty("IsSourceOf")
    IS_SOURCE_OF("IsSourceOf"),

    @JsonProperty("IsRequiredBy")
    IS_REQUIRED_BY("IsRequiredBy"),

    @JsonProperty("Requires")
    REQUIRES("Requires"),

    @JsonProperty("IsObsoletedBy")
    IS_OBSOLETED_BY("IsObsoletedBy"),

    @JsonProperty("Obsoletes")
    OBSOLETES("Obsoletes");

    private String name;

    RelationTypeDto(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
