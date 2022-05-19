package at.tuwien;

import lombok.*;
import org.springframework.core.io.InputStreamResource;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExportResource {

    private InputStreamResource resource;

    private String filename;

}
