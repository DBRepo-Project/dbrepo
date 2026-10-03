package at.ac.tuwien.ifs.dbrepo.core.api.replication;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SubsetResultManifestDto(
        @NotNull @Valid SubsetReplicationDto query,
        @NotNull @Size(max = 1048576) @JsonProperty("schema_json") String schemaJson,
        @NotNull @Pattern(regexp = "[0-9a-f]{64}") @JsonProperty("order_hash") String orderHash) {

    public record Column(String name, @JsonProperty("column_type") String columnType,
                         @JsonProperty("data_type") String dataType, String charset) { }

    public record Progress(boolean ready, @JsonProperty("next_row") long nextRow,
                           @JsonProperty("next_offset") long nextOffset) { }
}
