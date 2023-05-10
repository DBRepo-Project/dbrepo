package at.tuwien.api.database;

import lombok.*;

import jakarta.validation.constraints.NotNull;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DatabaseTransferDto {

    @NotNull
    private String username;

}
