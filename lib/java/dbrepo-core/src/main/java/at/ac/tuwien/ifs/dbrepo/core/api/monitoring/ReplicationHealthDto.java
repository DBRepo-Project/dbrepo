package at.ac.tuwien.ifs.dbrepo.core.api.monitoring;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class ReplicationHealthDto {

    @JsonProperty("status")
    @Schema(description = "Overall health status derived from all services", example = "UP")
    private String status;

    @JsonProperty("metadata_service")
    @Schema(description = "Health of the metadata-service")
    private ReplicationServiceHealthDto metadataService;

    @JsonProperty("data_service")
    @Schema(description = "Health of the data-service")
    private ReplicationServiceHealthDto dataService;

    @JsonProperty("replication_service")
    @Schema(description = "Health of the replication-service (self)")
    private ReplicationServiceHealthDto replicationService;

    @JsonProperty("broker")
    @Schema(description = "Health of the message broker used for replication")
    private ReplicationServiceHealthDto broker;
}


