package at.ac.tuwien.ifs.dbrepo.core.api.datacite;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.io.Serializable;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class DataCiteData<T> implements Serializable {

    private String id;

    private String type;

    private T attributes;
}
