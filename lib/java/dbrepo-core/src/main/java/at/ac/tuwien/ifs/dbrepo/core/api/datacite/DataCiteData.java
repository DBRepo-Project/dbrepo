package at.ac.tuwien.ifs.dbrepo.core.api.datacite;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.io.Serializable;
import java.util.Locale;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class DataCiteData<T> implements Serializable {

    private String id;

    private String type;

    private T attributes;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private JsonNode relationships;

    public DataCiteData(String id, String type, T attributes) {
        this(id, type, attributes, null);
    }

    public String registeringRepository() {
        if (relationships == null) return null;
        final JsonNode client = relationships.path("client").path("data");
        final String id = client.path("id").textValue();
        if (!"clients".equals(client.path("type").asText()) || id == null || id.isBlank() || !id.equals(id.strip())) {
            return null;
        }
        final String symbol = id.toUpperCase(Locale.ROOT);
        return symbol.length() <= 255 ? symbol : null;
    }
}
