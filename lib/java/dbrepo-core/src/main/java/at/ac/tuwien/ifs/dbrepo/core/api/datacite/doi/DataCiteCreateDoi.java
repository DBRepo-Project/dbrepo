package at.ac.tuwien.ifs.dbrepo.core.api.datacite.doi;

import lombok.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.extern.jackson.Jacksonized;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
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
