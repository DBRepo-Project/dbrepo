package at.ac.tuwien.ifs.dbrepo.core.api.datacite.doi;

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
public class DataCiteDoi implements Serializable {

    private String doi;
}
