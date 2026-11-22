package at.ac.tuwien.ifs.dbrepo.oaipmh;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.text.ParseException;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.regex.Pattern;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public abstract class OaiListParameters extends RequestParameters {

    public static final String SET_SPEC = "openaire_data";
    private static final DateTimeFormatter SECOND = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss'Z'")
            .withResolverStyle(ResolverStyle.STRICT);
    private static final String SPEC_PART = "[A-Za-z0-9_!~*'().-]+";
    private static final Pattern PREFIX = Pattern.compile(SPEC_PART);
    private static final Pattern SET = Pattern.compile(SPEC_PART + "(?::" + SPEC_PART + ")*");

    private String metadataPrefix;
    private String from;
    private String until;
    private String set;
    private String resumptionToken;

    public abstract String verb();

    public OaiErrorType argumentError() {
        if (resumptionToken != null) {
            return metadataPrefix != null || from != null || until != null || set != null
                    ? OaiErrorType.BAD_ARGUMENT : OaiErrorType.BAD_RESUMPTION_TOKEN;
        }
        if (metadataPrefix == null || !PREFIX.matcher(metadataPrefix).matches()
                || (set != null && !SET.matcher(set).matches())) {
            return OaiErrorType.BAD_ARGUMENT;
        }
        return "oai_dc".equals(metadataPrefix) || "oai_datacite".equals(metadataPrefix)
                ? null : OaiErrorType.CANNOT_DISSEMINATE_FORMAT;
    }

    public record DateRange(Instant from, Instant untilExclusive) {}

    public DateRange dateRange(boolean daysOnly) {
        if (from != null && until != null && from.length() != until.length()) {
            throw new DateTimeException("from and until must have the same granularity");
        }
        if (daysOnly && ((from != null && from.length() != 10) || (until != null && until.length() != 10))) {
            throw new DateTimeException("This repository supports day granularity");
        }
        final Instant start = parseDate(from);
        final Instant end = parseDate(until);
        if (start != null && end != null && start.isAfter(end)) {
            throw new DateTimeException("from follows until");
        }
        if (end == null) return new DateRange(start, null);
        return new DateRange(start, end.plus(1, until.length() == 10 ? ChronoUnit.DAYS : ChronoUnit.SECONDS));
    }

    private static Instant parseDate(String value) {
        if (value == null) return null;
        if (value.length() == 10) return LocalDate.parse(value).atStartOfDay().toInstant(ZoneOffset.UTC);
        if (value.length() == 20) return LocalDateTime.parse(value, SECOND).toInstant(ZoneOffset.UTC);
        throw new DateTimeException("Expected a UTC day or second");
    }

    public Date getFromDate() throws ParseException {
        return asDate(from);
    }

    public Date getUntilDate() throws ParseException {
        return asDate(until);
    }

    private Date asDate(String value) throws ParseException {
        if (value == null || value.isEmpty()) return null;
        try {
            return Date.from(parseDate(value));
        } catch (DateTimeException e) {
            throw new ParseException(e.getMessage(), 0);
        }
    }

    @Override
    public String getParametersString() {
        StringBuilder builder = new StringBuilder();
        appendIfNotEmpty(builder, "verb", verb());
        appendIfNotEmpty(builder, "metadataPrefix", metadataPrefix);
        appendIfNotEmpty(builder, "from", from);
        appendIfNotEmpty(builder, "until", until);
        appendIfNotEmpty(builder, "set", set);
        appendIfNotEmpty(builder, "resumptionToken", resumptionToken);
        return builder.toString();
    }
}
