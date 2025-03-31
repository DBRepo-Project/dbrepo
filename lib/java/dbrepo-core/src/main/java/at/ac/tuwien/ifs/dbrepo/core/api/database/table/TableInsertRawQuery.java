package at.ac.tuwien.ifs.dbrepo.core.api.database.table;

import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.Collection;
import java.util.List;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class TableInsertRawQuery {

    private String query;

    private List<Collection<Object>> values;

}
