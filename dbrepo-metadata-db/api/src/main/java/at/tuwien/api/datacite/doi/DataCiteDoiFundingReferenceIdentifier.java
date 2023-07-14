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
public class DataCiteDoiFundingReferenceIdentifier implements Serializable {

    private String funderIdentifier;

    private String funderIdentifierType;
}
