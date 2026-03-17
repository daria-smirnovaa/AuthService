package auth_service.util;

import auth_service.auth.SecurityConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtTokenUtils {

    public final SecurityConstants securityConstants;

    public String generateAccessToken(UserDetails userDetails) {
        return generateToken(userDetails, securityConstants.getAccessLifetime());
    }

    public String generateRefreshToken(UserDetails userDetails) {
        return generateToken(userDetails, securityConstants.getRefreshLifetime());
    }

    private String generateToken(UserDetails userDetails, Integer expiration) {
        Date issuedDate = new Date();
        Date expiredDate = new Date(issuedDate.getTime() + expiration);

        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .setIssuedAt(issuedDate)
                .setExpiration(expiredDate)
                .signWith(SignatureAlgorithm.HS256, securityConstants.getRefreshSecret())
                .compact();
    }

    public String getUsername(String token, String secret) {
        return getAllClaimsFromToken(token, secret).getSubject();
    }

    private Claims getAllClaimsFromToken(String token, String secret) {
        return Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token)
                .getBody();
    }
}