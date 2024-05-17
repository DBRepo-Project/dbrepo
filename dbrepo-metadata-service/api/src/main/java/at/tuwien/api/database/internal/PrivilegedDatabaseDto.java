package at.tuwien.api.database.internal;

import at.tuwien.api.container.internal.PrivilegedContainerDto;
import at.tuwien.api.database.DatabaseAccessDto;
import at.tuwien.api.database.ViewDto;
import at.tuwien.api.database.table.TableDto;
import at.tuwien.api.identifier.IdentifierDto;
import at.tuwien.api.user.UserDto;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class PrivilegedDatabaseDto {

    @NotNull
    private Long id;

    @NotBlank
    @Schema(example = "Air Quality")
    private String name;

    @NotBlank
    @JsonProperty("exchange_name")
    @Schema(example = "dbrepo")
    private String exchangeName;

    @JsonProperty("exchange_type")
    @Schema(example = "topic")
    private String exchangeType;

    @NotBlank
    @JsonProperty("internal_name")
    @Schema(example = "air_quality")
    private String internalName;

    @Schema(example = "Air Quality")
    private String description;

    private List<TableDto> tables;

    private List<ViewDto> views;

    @NotNull
    @JsonProperty("is_public")
    @Schema(example = "true")
    private Boolean isPublic;

    @NotNull
    private PrivilegedContainerDto container;

    private List<DatabaseAccessDto> accesses;

    private List<IdentifierDto> identifiers;

    @NotNull
    private UserDto creator;

    @NotNull
    private UserDto contact;

    @NotNull
    private UserDto owner;

    @ToString.Exclude
    private byte[] image;

    @NotNull
    @Schema(example = "2021-03-12T15:26:21Z")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant created;

}
