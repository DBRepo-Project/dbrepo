package at.tuwien;

import lombok.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;

import java.text.ParseException;
import java.util.Date;

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