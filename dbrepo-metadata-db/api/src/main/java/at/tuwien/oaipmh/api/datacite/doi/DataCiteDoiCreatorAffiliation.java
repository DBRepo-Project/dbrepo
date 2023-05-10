package at.tuwien.api.datacite.doi;

import lombok.*;

import java.io.Serializable;

@Data
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataCiteDoiCreatorAffiliation implements Serializable {

    private String affiliationIdentifier;

    private String affiliationScheme;

    private String name;

    private String schemeUri;
}
