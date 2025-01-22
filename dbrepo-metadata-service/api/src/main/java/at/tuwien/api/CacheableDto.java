package at.tuwien.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    private Instant lastRetrieved;

    @ToString.Exclude
    @JsonIgnore
    private String jdbcMethod;

    @ToString.Exclude
    @JsonIgnore
    private String host;

    @ToString.Exclude
    @JsonIgnore
    private Integer port;

    @ToString.Exclude
    @JsonIgnore
    private String username;

    @ToString.Exclude
    @JsonIgnore
    private String password;

    @ToString.Exclude
    @JsonIgnore
    private String database;

}
