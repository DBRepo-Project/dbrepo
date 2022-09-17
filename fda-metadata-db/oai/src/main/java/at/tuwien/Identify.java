package at.tuwien;

import lombok.*;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Identify {

    private String repositoryName;

    private String baseURL;

    @Builder.Default
    private String protocolVersion = "2.0";

    private String adminEmail;

    private String earliestDatestamp;

    private String deletedRecord;

    private String granularity;

}
