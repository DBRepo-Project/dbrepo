package at.tuwien.api;

import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.table.Table;
import at.tuwien.exception.TableNotFoundException;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Optional;

@Getter
@Setter
@Builder
public class CachedConnection {

    private ComboPooledDataSource dataSource;

    private Database database;

    private Instant lastUsed;

    public Table getTable(String internalName) throws TableNotFoundException {
        final Optional<Table> optional = database.getTables()
                .stream()
                .filter(t -> t.getInternalName().equals(internalName))
                .findFirst();
        if (optional.isEmpty()) {
            /* can never happen */
            throw new TableNotFoundException("Failed to find table with internal name " + internalName);
        }
        return optional.get();
    }

}
