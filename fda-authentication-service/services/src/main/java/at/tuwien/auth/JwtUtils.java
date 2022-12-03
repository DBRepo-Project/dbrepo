package at.tuwien.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Slf4j
@Component
public class JwtUtils {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration.ms}")
    private Integer expire;

    public String generateJwtToken(String username, Instant expire) {
        final Algorithm algorithm = Algorithm.HMAC512(secret);
        return JWT.create()
                .withSubject(username)
                .withIssuedAt(new Date())
                .withExpiresAt(Date.from(expire))
                .sign(algorithm);
    }

    public String generateJwtToken(String username) {
        return generateJwtToken(username, Instant.now().plus(expire, ChronoUnit.MILLIS));
    }

    public String getUserNameFromJwtToken(String token) {
        return JWT.decode(token)
                .getSubject();
    }

    public static String toHash(String token) {
        return DigestUtils.sha256Hex(token);
    }

    public boolean validateJwtToken(String authToken) {
        try {
            final DecodedJWT jwt = JWT.decode(authToken);
            return jwt.getExpiresAt().after(new Date());
        } catch (JWTDecodeException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
        }
        return false;
    }
}
