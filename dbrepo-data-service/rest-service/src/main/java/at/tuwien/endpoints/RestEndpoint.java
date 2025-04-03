package at.tuwien.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.api.user.UserDetailsDto;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.springframework.security.core.Authentication;

import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public abstract class RestEndpoint {

    public boolean hasRole(Principal principal, String role) {
        if (principal == null || role == null) {
            return false;
        }
        final Authentication authentication = (Authentication) principal;
        return authentication.getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals(role));
    }

    public boolean isSystem(Principal principal) {
        if (principal == null) {
            return false;
        }
        final Authentication authentication = (Authentication) principal;
        return authentication.getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("system"));
    }

    public UUID getId(Principal principal) {
        if (principal == null) {
            return null;
        }
        final Authentication authentication = (Authentication) principal;
        final UserDetailsDto user = (UserDetailsDto) authentication.getPrincipal();
        if (user.getId() == null) {
            return null;
        }
        return UUID.fromString(user.getId());
    }

    /* FIXME: Heap may run OOM */
    public List<Map<String, Object>> transform(Dataset<Row> dataset) {
        return dataset.collectAsList()
                .stream()
                .map(row -> {
                    final Map<String, Object> map = new LinkedHashMap<>();
                    for (int i = 0; i < dataset.columns().length; i++) {
                        if (row.get(i) == null) {
                            map.put(dataset.columns()[i], null);
                            continue;
                        }
                        try {
                            map.put(dataset.columns()[i], Integer.parseInt(String.valueOf(row.get(i))));
                            map.put(dataset.columns()[i], Double.parseDouble(String.valueOf(row.get(i))));
                        } catch (NumberFormatException e) {
                            /* ignore */
                        }
                        map.put(dataset.columns()[i], String.valueOf(row.get(i)));
                    }
                    return map;
                })
                .toList();
    }
}
