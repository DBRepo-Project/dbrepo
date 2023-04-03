package at.tuwien.datacite.doi;

import lombok.*;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
