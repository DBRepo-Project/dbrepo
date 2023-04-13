package at.tuwien.api.datacite;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DataCiteError {

    private String message;

    private Map<String, String> position;

}
