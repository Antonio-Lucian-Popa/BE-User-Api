package com.asusoftware.user_api.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.security.Key;
import java.util.*;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${application.security.jwt.secret-key}")
    private String secretKey;

    @Value("${application.security.jwt.expiration}")
    private long jwtExpiration;

    @Value("${application.security.jwt.refresh-token.expiration}")
    private long refreshExpiration;

    /**
     * Get the sign in key
     * @return sign in key
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject); // Subject will be the email(username) of the user
    }

    /**
     * Extract a claim from the jwt token
     * @param token jwt token
     * @param claimsResolver claims resolver
     * @param <T> type of the claim
     * @return claim
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateRefreshToken(
            UserDetails userDetails,
            UUID userId
    ) {
        return generateToken(userId, new HashMap<>(), userDetails, refreshExpiration);
    }

    /**
     * Generate a jwt token
     * @param userDetails user details
     * @return jwt token
     */
    public String generateToken(UserDetails userDetails, UUID userId) {
        return generateToken(userId, new HashMap<>(), userDetails, jwtExpiration);
    }

    /**
     * Generate a jwt token
     * @param extraClaims extra claims that we want to add to the token
     * @param userDetails user details
     * @return jwt token
     */
    public String generateToken(UUID userId, Map<String, Object> extraClaims, UserDetails userDetails, long expiration) {
        extraClaims.put("userId", userId);
        //extraClaims.put("userRoles", userDetails.getAuthorities());
        return Jwts
                .builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis())) // check if the token is expired
                        .setExpiration(new Date(System.currentTimeMillis() + expiration)) // 10 hours
                        .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                        .compact(); // generate and return the token
    }

    /**
     * Check if the token is valid
     * @param token jwt token
     * @param userDetails user details
     * @return true if the token is valid, false otherwise
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date()); // Check if the token is expired
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration); // Expiration will be the expiration date of the token
    }

    /**
     * Check if the token is valid
     * @param token jwt token
     * @return true if the token is valid, false otherwise
     */
    private Claims extractAllClaims(String token) {
        return Jwts
                .parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token) // Changed from parseClaimsJwt to parseClaimsJws
                .getBody();
    }


    /**
     * Get the sign in key
     * @return sign in key
     */
    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64URL.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
