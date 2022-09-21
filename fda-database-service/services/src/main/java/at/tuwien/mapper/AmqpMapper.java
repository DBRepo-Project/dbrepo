package at.tuwien.mapper;

import at.tuwien.api.user.ExchangeUpdatePermissionsDto;
import at.tuwien.entities.database.Database;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AmqpMapper {

    default String exchangeName(Database database) {
        return database.getInternalName();
    }

    default ExchangeUpdatePermissionsDto exchangeToExchangeUpdatePermissionsDto(String exchange) {
        return ExchangeUpdatePermissionsDto.builder()
                .exchange(exchange)
                .read(".*")
                .write(".*")
                .build();
    }

}
