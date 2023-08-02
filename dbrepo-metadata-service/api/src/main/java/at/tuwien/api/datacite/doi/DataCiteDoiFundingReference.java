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
public class DataCiteDoiFundingReference implements Serializable {

    private String funderName;

    private DataCiteDoiFundingReferenceIdentifier funderIdentifier;

    private String awardNumber;

    private String awardTitle;
}
