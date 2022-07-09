package at.tuwien.api.document.file;

import at.tuwien.api.document.links.LinksDto;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.*;

import java.time.Instant;


@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EntryDto {

    @Parameter(name = "key", description = "Filename")
    private String key;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSSSSS")
    @Parameter(name = "updated")
    private Instant updated;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSSSSS")
    @Parameter(name = "created")
    private Instant created;

    @Parameter(name = "metadata")
    private String metadata;

    @Parameter(name = "status", description = "Upload status")
    private String status;

    @Parameter(name = "links")
    private LinksDto links;
}
