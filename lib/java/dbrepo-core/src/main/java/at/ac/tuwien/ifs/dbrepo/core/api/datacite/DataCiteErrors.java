package at.ac.tuwien.ifs.dbrepo.core.api.datacite;

import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class DataCiteErrors {

    private List<DataCiteError> errors;

}
