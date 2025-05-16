package at.ac.tuwien.ifs.dbrepo.auth;

import at.ac.tuwien.ifs.dbrepo.core.api.auth.RealmAccessDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.UserDetailsDto;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.Verification;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.stream.Collectors;

@Slf4j
public class AuthTokenFilter extends OncePerRequestFilter {

    @Value("${dbrepo.jwt.public_key}")
    private String publicKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        final String jwt = parseJwt(request);
        if (jwt != null) {
            final UserDetails userDetails = verifyJwt(jwt);
            final UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        filterChain.doFilter(request, response);
    }

    public DecodedJWT decodeJwt(String token) {
        final KeyFactory kf;
        try {
            kf = KeyFactory.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            log.error("Failed to find RSA algorithm");
            throw new BadCredentialsException("Failed to find RSA algorithm", e);
        }
        final X509EncodedKeySpec keySpecX509 = new X509EncodedKeySpec(Base64.getDecoder().decode(publicKey));
        final RSAPublicKey pubKey;
        try {
            pubKey = (RSAPublicKey) kf.generatePublic(keySpecX509);
        } catch (InvalidKeySpecException e) {
            log.error("Provided public key is invalid");
            throw new BadCredentialsException("Provided public key is invalid", e);
        }
        final Algorithm algorithm = Algorithm.RSA256(pubKey, null);
        final Verification verification = JWT.require(algorithm);
        final JWTVerifier verifier = verification.build();
        return verifier.verify(token);
    }

    public UserDetails verifyJwt(String token) throws BadCredentialsException {
        final DecodedJWT jwt = decodeJwt(token);
        final RealmAccessDto realmAccess = jwt.getClaim("realm_access").as(RealmAccessDto.class);
        return UserDetailsDto.builder()
                .id(jwt.getClaim("uid").asString())
                .username(jwt.getClaim("preferred_username").asString())
                .authorities(Arrays.stream(realmAccess.getRoles()).map(SimpleGrantedAuthority::new).collect(Collectors.toList()))
                .build();
    }

    /**
     * Parses the token from the HTTP header of the request
     *
     * @param request The request.
     * @return The token.
     */
    public String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7, headerAuth.length());
        }
        return null;
    }
}