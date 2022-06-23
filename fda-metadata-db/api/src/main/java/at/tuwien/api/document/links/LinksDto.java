package at.tuwien.api.document.links;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.*;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LinksDto {

    @Parameter(name = "latest")
    private String latest;

    @Parameter(name = "versions")
    private String versions;

    @JsonProperty("self_html")
    @Parameter(name = "self html")
    private String selfHtml;

    @Parameter(name = "publish")
    private String publish;

    @JsonProperty("latest_html")
    @Parameter(name = "latest html")
    private String latestHtml;

    @Parameter(name = "self")
    private String self;

    @Parameter(name = "files")
    private String files;

    @Parameter(name = "commit")
    private String commit;

    @JsonProperty("access_links")
    @Parameter(name = "access links")
    private String accessLinks;

}
