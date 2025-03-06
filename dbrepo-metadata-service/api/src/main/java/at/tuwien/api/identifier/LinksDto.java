package at.tuwien.api.identifier;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;


@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class LinksDto {

    @NotNull
    @Schema(example = "http://example.com/api/")
    private String self;

    @NotNull
    @JsonProperty("self_html")
    @Schema(example = "http://example.com")
    private String selfHtml;

    @Schema(example = "http://example.com")
    private String data;

}
