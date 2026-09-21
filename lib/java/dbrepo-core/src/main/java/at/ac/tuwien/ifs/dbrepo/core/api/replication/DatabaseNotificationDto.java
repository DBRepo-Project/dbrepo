package at.ac.tuwien.ifs.dbrepo.core.api.replication;

import at.ac.tuwien.ifs.dbrepo.core.api.database.CreateDatabaseDto;
import jakarta.validation.Valid;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class DatabaseNotificationDto {
    UUID creationId;
    CreateDatabaseDto createDatabaseDto;
    @Valid
    ReplicationOwnerDto owner;

}
