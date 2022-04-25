package at.tuwien;

import lombok.*;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExportQueryRawQuery {

    private String query;

    private String path;

}
