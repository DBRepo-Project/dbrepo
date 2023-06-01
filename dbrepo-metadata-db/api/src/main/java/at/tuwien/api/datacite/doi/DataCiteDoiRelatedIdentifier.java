package at.tuwien.api.datacite.doi;

import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.io.Serializable;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class DataCiteDoiRelatedIdentifier implements Serializable {

    private String relatedIdentifier;

    private String relatedIdentifierType;

    private String relationType;

    private String resourceTypeGeneral;
}
