package at.tuwien.datacite.doi;

import lombok.*;

import java.io.Serializable;

@Data
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataCiteDoiRelatedIdentifier implements Serializable {

    private String relatedIdentifier;

    private String relatedIdentifierType;

    private String relationType;

    private String resourceTypeGeneral;
}
