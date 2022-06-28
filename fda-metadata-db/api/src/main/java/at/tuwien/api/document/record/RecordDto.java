package at.tuwien.api.document.record;

import at.tuwien.api.document.links.PersistentIdentifiersDto;
import at.tuwien.api.document.metadata.MetadataDto;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.*;

import javax.validation.constraints.NotNull;
import java.time.Instant;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RecordDto {

    @NotNull(message = "access is required")
    @Parameter(name = "access")
    private AccessOptionsDto access;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC+2")
    private Instant created;

    @JsonProperty("expires_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSSSSS", timezone = "UTC+2")
    private Instant expiresAt;

    @NotNull(message = "files options is required")
    @Parameter(name = "files options")
    private FilesOptionsDto files;

    @NotNull(message = "id is required")
    @Parameter(name = "id")
    private String id;

    @JsonProperty("is_published")
    @NotNull(message = "is published is required")
    @Parameter(name = "is published")
    private Boolean isPublished;

    @NotNull(message = "metadata is required")
    @Parameter(name = "metadata")
    private MetadataDto metadata;

    @JsonProperty("revision_id")
    @NotNull(message = "revision id is required")
    @Parameter(name = "revision id")
    private Long revisionId;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC+2")
    @Parameter(name = "updated date")
    private Instant updated;

    @NotNull(message = "versions is required")
    @Parameter(name = "revisions")
    private DraftVersionsDto versions;

    @NotNull(message = "pids is required")
    @Parameter(name = "pids")
    private PersistentIdentifiersDto pids;

}
