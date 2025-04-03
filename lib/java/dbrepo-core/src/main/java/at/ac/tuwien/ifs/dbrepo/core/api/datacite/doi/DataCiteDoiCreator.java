package at.ac.tuwien.ifs.dbrepo.core.api.datacite.doi;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import jakarta.validation.constraints.NotBlank;
import lombok.extern.jackson.Jacksonized;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class DataCiteDoiCreator implements Serializable {

    @NotBlank
    private String name;

    private String givenName;

    private String familyName;

    @NotNull
    private DataCiteNameType nameType;

    private List<DataCiteDoiCreatorAffiliation> affiliation;

    private List<DataCiteDoiCreatorNameIdentifier> nameIdentifier;
}
