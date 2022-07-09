package at.tuwien.api.document.file;

import at.tuwien.api.document.links.LinksDto;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.Instant;


@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileDto {

    @NotBlank
    @JsonProperty("bucket_id")
    @Parameter(name = "bucket id", description = "Bucket id.")
    private String bucketId;

    @NotBlank
    @Parameter(name = "file checksum", description = "File checksum.", example = "md5:ef8fcf1f046bb24f1db1f1a376ddbfbb")
    private String checksum;

    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX")
    @Parameter(name = "file creation timestamp")
    private Instant created;

    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX")
    @Parameter(name = "file updated timestamp")
    private Instant updated;

    @NotBlank
    @JsonProperty("file_id")
    @Parameter(name = "file id")
    private String fileId;

    @NotBlank
    @Parameter(name = "file key", example = "mock.png")
    private String key;

    @NotNull
    @Parameter(name = "file links")
    private LinksDto links;

    @Parameter(name = "file mimetype")
    private String mimetype;

    @Parameter(name = "file size")
    private Long size;

    @Parameter(name = "file status")
    private String status;

    @JsonProperty("storage_class")
    @Parameter(name = "file storage class", example = "S")
    private String storageClass;

    @JsonProperty("version_id")
    @Parameter(name = "file version id")
    private String versionId;
}
