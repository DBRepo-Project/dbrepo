package at.tuwien.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;

@Getter
@Setter
@EqualsAndHashCode
@ToString
public abstract class CacheableDto {

    @JsonProperty("last_retrieved")
    @Schema(example = "2025-01-23T12:09:01")
    private Instant lastRetrieved;

    @ToString.Exclude
    @Schema(example = "mariadb")
    private String jdbcMethod;

    @ToString.Exclude
    @Schema(example = "data-db")
    private String host;

    @ToString.Exclude
    @Schema(example = "3306")
    private Integer port;

    @ToString.Exclude
    @Schema(example = "username")
    private String username;

    @ToString.Exclude
    @JsonIgnore
    private String password;

    @ToString.Exclude
    @Schema(example = "air_quality")
    private String database;

}
