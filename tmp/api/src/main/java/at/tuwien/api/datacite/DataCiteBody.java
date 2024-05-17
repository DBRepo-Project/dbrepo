package at.tuwien.api.datacite;

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
public class DataCiteBody<T> implements Serializable {

    private DataCiteData<T> data;
}
