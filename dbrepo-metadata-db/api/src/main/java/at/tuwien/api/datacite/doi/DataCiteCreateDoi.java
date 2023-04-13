package at.tuwien.api.datacite.doi;

import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

@Data
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataCiteCreateDoi implements Serializable {

    private String url;

    private String prefix;

    private DataCiteDoiTypes types;

    private DataCiteDoiEvent event;

    private List<DataCiteDoiTitle> titles;

    @NotBlank
    private String publisher;

    @NotNull
    private Integer publicationYear;

    private Integer publicationMonth;

    private Integer publicationDay;

    private String language;

    private List<DataCiteDoiRights> rightsList;

    private List<DataCiteDoiCreator> creators;

    private List<DataCiteDoiRelatedIdentifier> relatedIdentifiers;
}
