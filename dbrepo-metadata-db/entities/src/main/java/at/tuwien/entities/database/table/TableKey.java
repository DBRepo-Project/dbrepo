package at.tuwien.entities.database.table;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class TableKey implements Serializable {

    private Long id;

    private Long tdbid;

}
