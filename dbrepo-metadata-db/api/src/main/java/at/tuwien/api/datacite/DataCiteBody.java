package at.tuwien.api.datacite;

import lombok.*;

import java.io.Serializable;

@Data
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataCiteBody<T> implements Serializable {

    private DataCiteData<T> data;
}
