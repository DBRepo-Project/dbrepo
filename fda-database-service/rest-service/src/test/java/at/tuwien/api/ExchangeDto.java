package at.tuwien.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties("arguments")
public class ExchangeDto {

    private String vhost;

    @JsonProperty("auto_delete")
    private Boolean autoDelete;

    private Boolean durable;

    private Boolean internal;

    private String name;

    private String type;

    @JsonProperty("user_who_performed_action")
    private String userWhoPerformedAction;

}
