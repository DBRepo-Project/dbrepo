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
public class DataCiteDoiCreatorAffiliation implements Serializable {

    private String affiliationIdentifier;

    private String affiliationScheme;

    private String name;

    private String schemeUri;
}
