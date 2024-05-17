package at.tuwien.api.datacite;

import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class DataCiteError {

    private String message;

    private Map<String, String> position;

}
