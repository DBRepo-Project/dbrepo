package at.ac.tuwien.ifs.dbrepo.core.api.replication;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class TableDeleteNotificationDto {

    private UUID databaseId;
    private UUID tableId;
    private Map<String, UUID> databaseReplicaIds;
    private Map<String, UUID> tableReplicaIds;
}
