package at.ac.tuwien.ifs.dbrepo.core.api.datacite.doi;

import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.io.Serializable;

@Getter
@Setter
@Builder
@EqualsAndHashCode
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
