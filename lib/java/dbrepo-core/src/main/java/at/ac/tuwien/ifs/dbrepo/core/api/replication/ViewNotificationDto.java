package at.ac.tuwien.ifs.dbrepo.core.api.replication;

import at.ac.tuwien.ifs.dbrepo.core.api.database.ViewDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.ReplicaLocation;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class ViewNotificationDto {
    UUID databaseId;
    UUID creationId;
    ViewDto viewDto;
    List<ReplicaLocation> replicas;
}
