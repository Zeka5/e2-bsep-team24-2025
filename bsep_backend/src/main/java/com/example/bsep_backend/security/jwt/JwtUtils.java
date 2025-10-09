package com.example.bsep_backend.security.jwt;

import com.example.bsep_backend.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class JwtUtils {
    private final JwtProperties jwtProperties;
    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateTokenWithUserInfo(User user, String sessionId) {
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("id", user.getId())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .claim("sessionId", sessionId)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtProperties.getExpirationMs()))
                .signWith(secretKey)
                .compact();
    }

    public String generateToken(String subject) {
        return Jwts.builder()
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtProperties.getExpirationMs()))
                .signWith(secretKey)
                .compact();
    }

    public String getEmailFromToken(String token) {
        return extractClaims(token, claims -> claims.get("email", String.class));
    }

    public <T>T extractClaims(String token, Function<Claims,T> claimsResolver) {
        return claimsResolver.apply(Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload());
    }

    public String getUsernameFromToken(String token){
        return extractClaims(token, Claims::getSubject);
    }

    public String getSessionIdFromToken(String token) {
        return extractClaims(token, claims -> claims.get("sessionId", String.class));
    }

    public Date getExpirationDate(){
        return new Date(System.currentTimeMillis() + jwtProperties.getExpirationMs());
    }

    private boolean isTokenExpired(String token){
        try{
            return extractClaims(token, Claims::getExpiration).before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return true;
        }
    }

    public boolean isTokenValid(String token){
        try{
            return(!isTokenExpired(token));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
