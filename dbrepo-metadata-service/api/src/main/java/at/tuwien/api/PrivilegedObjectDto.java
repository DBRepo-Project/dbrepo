package at.tuwien.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;

@Getter
@Setter
@ToString
public abstract class PrivilegedObjectDto {

    @JsonProperty("last_retrieved")
    private Instant lastRetrieved;

}
