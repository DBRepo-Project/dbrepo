package at.ac.tuwien.ifs.dbrepo.endpoints;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.jwt.Jwt;

import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    public String getUsername(Principal principal) {
        if (principal == null) {
            return null;
        }
        final Authentication authentication = (Authentication) principal;
        if (authentication.getPrincipal() instanceof Jwt user) {
            if (user.getClaimAsString("preferred_username") == null) {
                throw new IllegalArgumentException("Principal has no username");
            }
            return user.getClaimAsString("preferred_username");
        } else if (authentication.getPrincipal() instanceof User user) {
            if (user.getUsername() == null) {
                throw new IllegalArgumentException("Principal has no username");
            }
            return user.getUsername();
        }
        throw new IllegalArgumentException("Unknown principal instance: " + authentication.getPrincipal().getClass());
    }

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
                            continue;
                        } catch (NumberFormatException e0) {
                            try {
                                map.put(dataset.columns()[i], Double.parseDouble(String.valueOf(row.get(i))));
                                continue;
                            } catch (NumberFormatException e1) {
                                /* ignore */
                            }
                        }
                        map.put(dataset.columns()[i], String.valueOf(row.get(i)));
                    }
                    return map;
                })
                .toList();
    }
}
