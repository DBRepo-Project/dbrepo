package at.tuwien.api.document.metadata;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MetadataDto {

    @JsonProperty("resource_type")
    @NotNull(message = "enabled is required")
    @Parameter(name = "files enabled", description = "Should (and can) files be attached to this record or not.")
    private ResourceTypeDto resourceType;

    /**
     * The field is compatible with 2. Creator in DataCite. In addition we are adding the possiblity of associating a
     * role (like for contributors). This is specifically for cases where e.g. an editor needs to be credited for the
     * work, while authors of individual articles will be listed under contributors.
     */
    @NotNull(message = "creators is required")
    @Parameter(name = "creators")
    private List<CreatorDto> creators;

    /**
     * The fields is compatible with 3. Title in DataCite. Compared to DataCite, the field does not support specifying
     * the language of the title.
     */
    @NotBlank(message = "title is required")
    @Parameter(name = "title")
    private String title;

    /**
     * The field is compatible 5. PublicationYear in DataCite. In case of time intervals, the earliest date is used
     * for DataCite.
     */
    @JsonProperty("publication_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @NotNull(message = "publication date is required")
    @Parameter(name = "publication date")
    private Date publicationDate;
}
