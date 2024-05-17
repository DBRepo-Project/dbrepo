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
public class DataCiteDoiCreatorNameIdentifier implements Serializable {

    private String schemeUri;

    private String nameIdentifier;

    private String nameIdentifierScheme;
}
