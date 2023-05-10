package at.tuwien.api.datacite.doi;

import lombok.*;

import java.io.Serializable;

@Data
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataCiteDoiCreatorNameIdentifier implements Serializable {

    private String schemeUri;

    private String nameIdentifier;

    private String nameIdentifierScheme;
}
