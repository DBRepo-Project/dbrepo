package at.tuwien.api.datacite.doi;

import lombok.*;
import lombok.extern.jackson.Jacksonized;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serializable;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class DataCiteDoiFundingReferenceIdentifier implements Serializable {

    @Field(name = "funder_identifier", type = FieldType.Text)
    private String funderIdentifier;

    @Field(name = "funder_identifier_type", type = FieldType.Keyword)
    private String funderIdentifierType;
}
