package at.tuwien.api.container.image;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigInteger;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ImageDto {

    @NotNull
    private Long id;

    @NotBlank
    @Schema(example = "mariadb")
    private String repository;

    @NotBlank
    @Schema(example = "10.5")
    private String tag;

    @NotBlank
    @JsonProperty("driver_class")
    @Schema(example = "org.mariadb.jdbc.Driver")
    @org.springframework.data.annotation.Transient
    private String driverClass;

    @JsonProperty("date_formats")
    private List<ImageDateDto> dateFormats;

    @NotBlank
    @Schema(example = "org.hibernate.dialect.MariaDBDialect")
    @org.springframework.data.annotation.Transient
    private String dialect;

    @NotBlank
    @JsonProperty("jdbc_method")
    @Schema(example = "mariadb")
    @org.springframework.data.annotation.Transient
    private String jdbcMethod;

    @Schema(example = "sha256:c5ec7353d87dfc35067e7bffeb25d6a0d52dad41e8b7357213e3b12d6e7ff78e")
    private String hash;

    @Schema(example = "2021-03-12T15:26:21.678396092Z")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant compiled;

    @Schema(example = "314295447")
    private BigInteger size;

    @NotNull
    @JsonProperty("default_port")
    @Schema(example = "3306")
    private Integer defaultPort;

    @NotNull
    @org.springframework.data.annotation.Transient
    private List<ImageEnvItemDto> environment;

}
