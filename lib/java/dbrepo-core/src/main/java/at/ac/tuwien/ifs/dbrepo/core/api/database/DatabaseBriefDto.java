package at.ac.tuwien.ifs.dbrepo.core.api.database;

import at.ac.tuwien.ifs.dbrepo.core.api.identifier.IdentifierBriefDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.UserBriefDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class DatabaseBriefDto {

    @NotNull
    @Schema(example = "fc29f89c-86a8-4020-9e36-4d954736c6cc")
    private UUID id;

    @NotBlank
    @Schema(description = "The name", example = "Air Quality")
    private String name;

    @NotBlank
    @JsonProperty("internal_name")
    @Schema(description = "The machine-friendly database name", example = "air_quality")
    private String internalName;

    @Schema(description = "The comment", example = "Air Quality")
    private String description;

    @NotNull
    @JsonProperty("is_public")
    @Schema(description = "The visibility; if true, The will be displayed publicly and is searchable", example = "true")
    private Boolean isPublic;

    @NotNull
    @JsonProperty("is_schema_public")
    @Schema(description = "The insights; if true, The schema will be displayed publicly and is searchable", example = "true")
    private Boolean isSchemaPublic;

    @NotNull
    private List<IdentifierBriefDto> identifiers;

    @NotNull
    private UserBriefDto contact;

    @NotNull
    @JsonProperty("owned_by")
    @Schema(description = "The owner username", example = "foobar")
    private String ownedBy;

    @JsonProperty("preview_image")
    @Schema(description = "The preview image", example = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABkAAAAXCAMAAADJPRQhAAAAAXNSR0IB2cksfwAAAAlwSFlzAAALEwAACxMBAJqcGAAAAXRQTFRFAAAAAGaZAGaZAGaZAGaZAGaZAGaZAGaZAGaZAGaZAGaZAGaZAGaZAGaZAGaZAGaZAGaZAGaZAGaZAGaZAGaZAGaZAGaZAGaZAGaZAWaZOYiwb6nGfLHLX5+/GnWjAGaZPYux1OXu/////P3+pMnbEnGgAGaZudXj/v//UJa5AGaZaaXD+Pv89fn7utbjs9HhzOHrI3unAGaZbKfFKoCqibjQrM7eLoKsM4WtPIqxCmydAGaZAGaZvNfk1ebubajFCWucjrvS5vD12+nxvtjlDG2eAGaZN4ev0ePtG3ak4O3zAWeay+DqUZe5AGaZAGaZrs/fQ460GXWjFnOiA2iax97pU5i6AGaZAGaZ/f7+1+fvLYGrmcLW0+XtwNnm+/39YaHAAGaZVpq8pcnbjbvSTpW4WJu8T5W52OjvRY+0AGaZAGaZRY+1MIOsAGaZAGaZAGaZAGaZAGaZAGaZAGaZAGaZAGaZAGaZAGaZAGaZAGaZAGaZI9A4qQAAAHx0Uk5TAAE8k97224ksiAmb+v/0hwP+jkzy+5eu5/////////f////////j////vf////////+F//////////8o5v///////////33/////////Esr/////////D9r//////////9H//////////yHH//8I1tkMjwWB/Gj4V0PTRBXfRw8AAAFGSURBVHicbZDPK0RxFMXPkdAzMszCj0KUWdgQ2ZhsKCllpexYSGkWSkr8AUpKymKyErJRbJTEQjZKWckOC0WxQskzGua67/v9vhlqzuae8/287rv3EqBTGqoy/ogRCE8fgzctb9HgSaXFZ/Tdg1Uxs0Xy7YJfwRi/XNBOkg59qbDGR0F5rP0oTMoZobMRM+BrSIRNL9bFzITKnm1OM/5kXX3GlBI+2lzHtgfrGulrb9WdzQ3svLUu7i5x7f7TygSvjOsIj3RhYrsQA5dm7577Zj1PFXlm7tF9QgzzNPD9+vmndzzIoyD1yYGOOspD9ZW9rtu+hiHZRbBEIr4HdLXomtUB2sHIzTkMQZLbuauMc3NMUnAE09zIX2wiu4YcwcxWJgSTsoo/BLPkelCnRFbwjwBz1O5JWQ5znmBeUkkuoQDBArmYT7/cX1c496CkMgAAAABJRU5ErkJggg==")
    private String previewImage;

}
