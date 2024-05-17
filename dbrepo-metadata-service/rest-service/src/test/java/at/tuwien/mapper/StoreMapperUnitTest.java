package at.tuwien.mapper;

import at.tuwien.test.AbstractUnitTest;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Log4j2
public class StoreMapperUnitTest extends AbstractUnitTest {

    private final DateTimeFormatter mariaDbFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss[.SSS]")
            .withZone(ZoneId.of("UTC"));

    @Test
    public void mapMariaDbInstant_succeeds() {
        final String timestamp = "2023-01-08 08:49:29";
        final Instant compare = Instant.ofEpochSecond(1673167769);

        /* test */
        final Instant response = LocalDateTime.parse(timestamp, mariaDbFormatter)
                .atZone(ZoneId.of("UTC"))
                .toInstant();
        assertEquals(compare, response);
    }

}
