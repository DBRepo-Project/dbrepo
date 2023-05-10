package at.tuwien.api.database.table;

import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.Collection;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class TableInsertRawQuery {

    private String query;

    private List<Collection<Object>> values;

}
