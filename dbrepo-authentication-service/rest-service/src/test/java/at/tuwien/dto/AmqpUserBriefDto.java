package at.tuwien.dto;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class AmqpUserBriefDto {

    private String name;

    private String[] tags;

}
