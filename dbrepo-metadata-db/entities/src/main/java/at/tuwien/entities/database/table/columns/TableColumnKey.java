package at.tuwien.entities.database.table.columns;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class TableColumnKey implements Serializable {

    private Long id;

    private Long cdbid;

    private Long tid;

}
