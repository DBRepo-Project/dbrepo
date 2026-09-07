package at.ac.tuwien.ifs.dbrepo.service;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ReplicationOutboxStatusDto(
        @JsonProperty("replication_service") ReplicationOutboxSummaryDto replicationService,
        @JsonProperty("metadata_service") ReplicationOutboxSummaryDto metadataService,
        @JsonProperty("data_service") ReplicationOutboxSummaryDto dataService) {
}
