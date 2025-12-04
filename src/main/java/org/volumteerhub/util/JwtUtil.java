package org.volumteerhub.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.io.Decoders;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Date;

public class JwtUtil {

    private static final String SECRET_KEY_STRING =
            "thisisasecurekeyforvolumteerhubthatshouldbeverylongandkeptsecret";

    private static final Key KEY = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET_KEY_STRING));

    private static final long EXPIRATION_MS = 1000 * 60 * 60 * 24; // 24h

    /**
     * Generates a JWT token for the given username.
     * Uses the modern Jwts.builder() style.
     */
    public static String generateToken(String username) {
        Date now = new Date();
        Date expiration = new Date(System.currentTimeMillis() + EXPIRATION_MS);

        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(KEY) // Corrected signature
                .compact();
    }

    /**
     * Extracts the username from a JWT token.
     */
    public static String getUsernameFromToken(String token) {
        return Jwts.parser()
                .verifyWith((SecretKey) KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    /**
     * Validate JWT
     * @param token the token need to be validated
     * @return true if token is valid, false if not.
     */
    public static boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith((SecretKey) KEY)
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}