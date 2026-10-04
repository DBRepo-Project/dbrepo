package at.ac.tuwien.ifs.dbrepo.core.api.replication;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record AddReplicaDto(@NotBlank @JsonProperty("replica_url") String replicaUrl) {
}
