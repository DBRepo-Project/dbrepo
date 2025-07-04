package at.ac.tuwien.ifs.dbrepo.core.api.identifier.ld;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.LinkedList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class LdDatasetDto {

    @NotNull
    @JsonProperty("@context")
    private String context;

    @NotNull
    @JsonProperty("@type")
    private String type;

    @NotNull
    private String name;

    @NotNull
    private String description;

    @NotNull
    private String url;

    @NotNull
    private List<String> identifier = new LinkedList<>();

    private String license;

    @NotNull
    private List<LdCreatorDto> creator = new LinkedList<>();

    @NotNull
    private String citation;

    @NotNull
    private List<LdDatasetDto> hasPart = new LinkedList<>();

    @NotNull
    private String temporalCoverage;

    @NotNull
    private Instant version;

}
