package at.tuwien.api.database.table;

import lombok.*;

import java.util.Collection;
import java.util.List;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TableInsertRawQuery {

    private String query;

    private List<Collection<Object>> values;

}
