package at.ac.tuwien.ifs.dbrepo.core.api.replication;

import at.ac.tuwien.ifs.dbrepo.core.entity.database.ReplicationAccessStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class ReplicationAccessDto {

    @JsonProperty("database_id")
    private UUID databaseId;

    @JsonProperty("database_name")
    private String databaseName;

    @JsonProperty("creation_location")
    private String creationLocation;

    @JsonProperty("origin_owner")
    private ReplicationOwnerDto originOwner;

    private ReplicationAccessStatus status;

    @JsonProperty("local_username")
    private String localUsername;

}
