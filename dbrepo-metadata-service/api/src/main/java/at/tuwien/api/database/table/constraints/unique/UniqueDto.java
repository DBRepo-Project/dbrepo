
package at.tuwien.api.database.table.constraints.unique;

import at.tuwien.api.database.table.TableDto;
import at.tuwien.api.database.table.columns.ColumnDto;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class UniqueDto {

    @Id
    @NotNull
    @Field(name = "id", type = FieldType.Keyword)
    private Long uid;

    @NotNull
    @org.springframework.data.annotation.Transient
    private TableDto table;

    @NotNull
    @org.springframework.data.annotation.Transient
    private List<ColumnDto> columns;
}
