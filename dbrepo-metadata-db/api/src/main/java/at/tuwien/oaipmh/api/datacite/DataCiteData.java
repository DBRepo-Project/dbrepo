package at.tuwien.api.datacite;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.io.Serializable;

@Data
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class DataCiteData<T> implements Serializable {

    private String id;
    private String type;
    private T attributes;
}
