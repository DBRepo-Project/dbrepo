package at.tuwien;

import lombok.*;
import org.springframework.core.io.InputStreamResource;

@Getter
@Setter
@ToString
@Builder
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class ExportResourceDto {

    private InputStreamResource resource;

    private String filename;

}
