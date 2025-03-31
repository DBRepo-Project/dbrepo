package at.ac.tuwien.ifs.dbrepo.oaipmh;

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
        appendIfNotEmpty(builder, "metadataPrefix", metadataPrefix);
        appendIfNotEmpty(builder, "identifier", identifier);
        return builder.toString();
    }

}