package at.tuwien.oaipmh;

import lombok.*;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OaiRecordParameters extends RequestParameters {

    private String identifier;
    private String metadataPrefix;

    @Override
    public String getParametersString() {
        StringBuilder builder = new StringBuilder();
        appendIfNotEmpty(builder, "verb", "GetRecord");
        appendIfNotEmpty(builder, "metadataPrefix", this.getMetadataPrefix());
        appendIfNotEmpty(builder, "identifier", this.getIdentifier());

        return builder.toString();
    }

}