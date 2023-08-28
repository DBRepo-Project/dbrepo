package at.tuwien.api.datacite.doi;

import lombok.*;

import jakarta.validation.constraints.NotNull;
import lombok.extern.jackson.Jacksonized;

import java.io.Serializable;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class DataCiteDoiTypes implements Serializable {

    public static final DataCiteDoiTypes DATASET = DataCiteDoiTypes.builder().resourceTypeGeneral("Dataset").build();

    @NotNull
    private String resourceTypeGeneral;

    private String resourceType;

    private String schemaOrg;

    private String bibtex;

    private String citeproc;

    private String ris;
}
