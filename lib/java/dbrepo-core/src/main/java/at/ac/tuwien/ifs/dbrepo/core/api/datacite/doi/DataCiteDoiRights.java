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
public class DataCiteDoiRights implements Serializable {

    private String rights;

    private String rightsUri;

    private String lang;
}
