package at.tuwien.api.datacite.doi;

import lombok.*;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.List;

@Data
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataCiteDoiCreator implements Serializable {

    @NotBlank
    private String name;

    private String givenName;

    private String familyName;

    private List<DataCiteDoiCreatorAffiliation> affiliation;

    private List<DataCiteDoiCreatorNameIdentifier> nameIdentifiers;
}
