package at.ac.tuwien.ifs.dbrepo.core.api.replication;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;
import java.nio.charset.StandardCharsets;

public record DatabaseBootstrapDto(@NotNull UUID requestId, @NotBlank String targetSite,
                                   @NotNull @Valid DatabaseNotificationDto database,
                                   @NotNull List<@Valid TableNotificationDto> tables,
                                   @NotNull List<@Valid ViewNotificationDto> views) {
    public static UUID idFor(UUID databaseId, String targetSite) {
        return UUID.nameUUIDFromBytes(("bootstrap:" + databaseId + ":" + targetSite)
                .getBytes(StandardCharsets.UTF_8));
    }
}
