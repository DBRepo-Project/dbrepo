package at.ac.tuwien.ifs.dbrepo.oaipmh;

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
public class OaiListIdentifiersParameters extends RequestParameters {

    private static final String[] DATE_FORMATS = {"yyyy-MM-dd'T'HH:mm'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd"};
    private String metadataPrefix;
    private String from;
    private String until;
    private String set;
    private String resumptionToken;

    public Date getFromDate() throws ParseException {
        if (StringUtils.isNotEmpty(from)) {
            return DateUtils.parseDate(from, DATE_FORMATS);
        }
        return null;
    }

    public Date getUntilDate() throws ParseException {
        if (StringUtils.isNotEmpty(until)) {
            return DateUtils.parseDate(until, DATE_FORMATS);
        }
        return null;
    }

    @Override
    public String getParametersString() {
        StringBuilder builder = new StringBuilder();
        appendIfNotEmpty(builder, "verb", "ListIdentifiers");
        appendIfNotEmpty(builder, "metadataPrefix", metadataPrefix);
        appendIfNotEmpty(builder, "from", from);
        appendIfNotEmpty(builder, "until", until);
        appendIfNotEmpty(builder, "set", set);
        appendIfNotEmpty(builder, "resumptionToken", resumptionToken);
        return builder.toString();
    }

}