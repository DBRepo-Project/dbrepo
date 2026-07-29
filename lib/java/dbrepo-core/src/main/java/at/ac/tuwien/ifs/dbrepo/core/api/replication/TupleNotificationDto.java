package at.ac.tuwien.ifs.dbrepo.core.api.replication;

import java.util.Map;


import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class TupleNotificationDto {
    private String tableName;
    private Map<String, Object> tupleData;
    private String timestamp;

    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }

    public Map<String, Object> getTupleData() { return tupleData; }
    public void setTupleData(Map<String, Object> tupleData) { this.tupleData = tupleData; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}