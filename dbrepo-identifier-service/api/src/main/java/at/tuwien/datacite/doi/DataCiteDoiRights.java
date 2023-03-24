package at.tuwien.datacite.doi;

import lombok.*;

import java.io.Serializable;

@Data
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataCiteDoiRights implements Serializable {

    private String rights;

    private String rightsUri;

    private String lang;
}
