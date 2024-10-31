package com.asiancuisine.asiancuisine.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

public class JwtUtil {
    /**
     * generate jwt
     * Hs256 algorithm, fixed secret key
     *
     * @param secretKey jwt secret key
     * @param ttlMillis jwt expired time in milliseconds
     * @param claims    jwt encapsulated info
     * @return
     */
    public static String createJWT(String secretKey, long ttlMillis, Map<String, Object> claims) {
        // specify the signing algorithm
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;

        // the time that generate the JWT
        long expMillis = System.currentTimeMillis() + ttlMillis;
        Date exp = new Date(expMillis);

        // Set JWT body
        JwtBuilder builder = Jwts.builder()
                .setClaims(claims)
                .signWith(signatureAlgorithm, secretKey.getBytes(StandardCharsets.UTF_8))
                .setExpiration(exp);

        return builder.compact();
    }

    /**
     * Token Decryption
     *
     * @param secretKey jwt secret key
     * @param token     generated jwt token
     * @return
     */
    public static Claims parseJWT(String secretKey, String token) {
        // Get DefaultJwtParser
        Claims claims = Jwts.parser()
                .setSigningKey(secretKey.getBytes(StandardCharsets.UTF_8))
                .parseClaimsJws(token).getBody();
        return claims;
    }

}
